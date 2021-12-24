



## SpringBoot Thread

<hr>

##### 로그추적기

<HR>

> 이번 로그추적기에 대한 로직은 단순 스프링의 싱글톤으로 만들어낸 로그추적기이다.
>
> 나중에 AOP를 쓰거나 쓰레드를 이용해 로그를 뽑아보는 대신 기초부터 하고자 스프링 자체의 COMPONENT기능만 이용하여 최대한 로직은 지저분 하지만 스펙의 힘을 빌리지 않고 사용해서 보여주는 예를 적었다.

서비스가 커져서 어플리케이션 호출 전에 미리 로그를 남겨두고 자동화 하자.

로그추적기를 개발하면서 로그의 로그를 이어 받아 깊이를 나타내고 예외처리 역시 깔끔하게 띄워보도록 하자.

일단 기본적인 어플리케이션의 틀을 짰다.

```java
@RestController
@RequiredArgsConstructor
public class OrderControllerV0 {

    private final OrderServiceV0 orderServiceV0;

    @GetMapping("/v0/request")
    public String request(String itemId){
        orderServiceV0.orderItem(itemId);
        return "ok";
    }
}
```

우선 컨트롤러에서 get방식으로 인자를 받았다.

```java
@Service
@RequiredArgsConstructor
public class OrderServiceV0 {
    private final OrderRepositoryV0 orderRepositoryV0;
    public void orderItem(String itemId){
        orderRepositoryV0.save(itemId);
    }
}
```

서비스 단에서는 일반적인 호출로 repository를 받아서 save만 호출해주었다.

```java
@Repository
@RequiredArgsConstructor
public class OrderRepositoryV0 {
    public void save(String itemId){
        if(itemId.equals("ex")){
            throw new IllegalStateException("예외 발생!");
        }
        sleep(1000);
    }
    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }
}
```

최종적인 레포지토리 단에서는 저장하는 로직은 없지만 해당 스레드에 대해 1초간 정지할 수 있는 로직을 한줄 주었다.

일반적인 로그 추적기 적용에 앞서서 우리가 띄울 트랜잭션ID에 대한 객체를 만들어주어야 한다.

```java
@Getter
public class TraceId {
    private String id;
    private int level;

    public TraceId() {
        this.id = createId();
        this.level = 0;
    }

    private String createId() {
        return UUID.randomUUID().toString().substring(0,8);
    }

    private TraceId(String id, int level){
        this.id = id;
        this.level = level;
    }

    public TraceId createNextId(){
        return new TraceId(id,level+1);
    }
}
```

트랜잭션 ID는 여러 의미로 쓰일 수 있지만 내가 짠 코드에 대해서 트랜잭션은 하나의 HTTP요청에 대한 구분을 할 수 있는 것에 대한 트랜잭션 아이디라고 할 것이다.

```java
@Getter
public class TraceStatus {
    private TraceId traceId;
    private Long startTimeMs;
    private String message;

    public TraceStatus(TraceId traceId, Long startTimeMs, String message) {
        this.traceId = traceId;
        this.startTimeMs = startTimeMs;
        this.message = message;
    }
}
```

그리고 그 트랜잭션 아이디를 감싸고 있는 트랜잭션 상태정보에 대한 클래스도 만들어주어야 한다.

- startTimeMs 는 해당 요청이 발생했을 때, 시작 시간을 나타낸다. (진행시간을 얻기 위한 필드.)
- message 는 해당 요청이 발생했을 때, 같은 메세지를 반환하기 위해 만들어 놓은 message 이기도 하며 해당요청이 처음들어올 때 message를 띄우기도 하는 역할을 한다.
- traceID 는 해당 요청에 대한 트랜잭션 ID를 띄우기 위해 LEVEL(요청의 깊이)와 그 요청에 대한 고유의 ID를 담고있다.



##### 로그추적기 V1

<HR>

로그 추적기를 만들기 위해서 필요한 핵심로직을 가지고 있는 컴포넌트에 대해서 

```java
@Slf4j
@Component
public class HelloTraceV1 {

    private static final String START_PREFIX = "-->";
    private static final String COMPLETE_PREFIX = "<--";
    private static final String EX_PREFIX = "<X-";

    public TraceStatus begin(String message){
        TraceId traceId = new TraceId(); //begin이 들어오면 traceId를 새로 만들어낸다.
        Long startTimeMs = System.currentTimeMillis();
        log.info("[{}] {} {}", traceId.getId(),
                 addSpace(START_PREFIX, traceId.getLevel()), message);
        
        return new TraceStatus(traceId, startTimeMs, message);
    }


    public void end(TraceStatus status){
        complete(status,null);
    }
    
    public void exception(TraceStatus status,Exception e){
        complete(status,e);
    }

    private void complete(TraceStatus status, Exception e) {
        Long stopTime = System.currentTimeMillis();
        long resultTimeMs = stopTime - status.getStartTimeMs();
        TraceId traceId = status.getTraceId();
        if(e == null){
            log.info("[{}] {} {} time={}ms",
            status.getTraceId().getId(),
            addSpace(COMPLETE_PREFIX,traceId.getLevel()), 
            status.getMessage(), resultTimeMs);
        }else{
            log.info("[{}] {} {} ex={}",
            status.getTraceId().getId(),
            addSpace(EX_PREFIX,traceId.getLevel()),
            status.getMessage(),
            e.toString());
        }
    }

    private static String addSpace(String prefix, int level){
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i<level; i++){
            sb.append((i==level-1)?"|"+prefix:"|    ");
        }
        return sb.toString();
    }
}
```

우선 첫번째 begin 메서드에서 정리를 해보려고 한다.

```java
    public TraceStatus begin(String message){
        TraceId traceId = new TraceId();
        Long startTimeMs = System.currentTimeMillis();
        
        log.info("[{}] {} {}", traceId.getId(),
                 addSpace(START_PREFIX, traceId.getLevel()),
                 message);
        
        return new TraceStatus(traceId, startTimeMs, message);
    }
```

begin은 일단 처음 http요청이 들어오기 시작하면 message를 인자로 받아서 요청 메세지 내용과 처음 만들어진 TraceID에서 level에 대한 필드값은 정수형 primitive 타입의 디폴트 값은 0이기 때문에 `level은 0`이 유지될 것이고, id값은 자동으로 `UUID.random().toString()`으로 인해서 생성될 것이다.

컨트롤러의 내용을 한번 보겠다.

` private final HelloTraceV1 helloTraceV1;` 로 빈에 대한 주입을 직접 받고, 

```java
@GetMapping("/v1/request")
public String request(String itemId){
    TraceStatus status = null;
    try{
        status = helloTraceV1.begin("request OrderControllerV1");
        orderServiceV1.orderItem(itemId);
        helloTraceV1.end(status);
        return "ok";
    }catch (Exception e){
     	//오류가 났을 시에는 complete메서드 안의 if 분기를 타서 에러가 없으면 message를 출력하고
        //에러가 생겼을 시에는 e.getMessage 를 통한 오류를 출력합니다.
        helloTraceV1.exception(status, e);
        throw e;
    }
}
```

앞에서 설명을 못 한 end 메서드와 exception 메서드가 두개가 존재한다. 두 메서드의 코드는 공통부분이 있어서 하나의 메서드로 리팩토링하여 이름은 `complete(Status status, Exception e)` 메서드로 만들어 주었다.

내부의 내용을 살펴보자면, status 객체에는 해당 `begin(String message)` 의 메서드가 실행이 되면서 현재 시간에 대해서 status를 만들어주면서 `System.currentTimeMillis();` 를 인자로 넣어주었다. 

그렇기 때문에 status 를 인자로 받아서 end 메서드가 begin이 끝나고 실행하는 메서드이기 때문에 <b> begin이 직접 리턴했던 status를 객체로 받아 </b> `long resultTimeMs = stopTime - status.getStartTimeMs();` 로 반환 결과를 출력한다.

그리고 status객체 안에 traceID가 있었다. LEVEL과 ID로 구성되어있는 이 객체에서 ID를 뽑아내고 status에서 message를 뽑아내서 `log.info();` 로 로그를 출력하는데 addSpace에 대한건 레벨단의 문제기 때문에 뒤에서 설명하도록 하겠다.

> 🧨여기서 중요한 점은 `throw e;` 는 try catch 가 분기를 하면서 오류에 대해 정상적인 흐름으로 바꾸어 버리는데 어플리케이션의 흐름을 내가 직접 바꾸는 것은 좋지 않다. 에러를 먹고 정상적인 요청으로 반환을 하게 되면 혼동을 줄 수 있기 때문이다.



*[8fe7bbff]  request OrderControllerV1*
*[8251cfd5]  request OrderServiceV1*
*[17348592]  request OrderRepositoryV1*
*[17348592]  request OrderRepositoryV1 time=1007ms*
*[8251cfd5]  request OrderServiceV1 time=1007ms*
*[8fe7bbff]  request OrderControllerV1 time=1008ms*

의 결과가 나오게 되는데 service와 repository는 `package hello.advanced.app.v1;` 의 패키지 안에서 보도록 하자.

> 여기서 http하나의 요청에 대해 트랜잭션 id값도 다르고 깊이도 출력하지 않는다. 다음 v2에 대해서는 깊이와 id값의 유지를 해볼 것이다.
>
> 위사항을 해결하기 위해서는 로그에 대한 문맥(contex)정보를 가져와서 이전에 어떤 트랜잭션을 썼고 level이 뭐였는지를 전달해야한다.



##### 로그추적기V2

<hr>

기존 traceID를 유지하고 LEVEL의 정보를 추가하여 깔끔하게 볼 수 있도록 하기 위해 코드를 좀 더 수정해 보았다.

✍️ 여기서 LEVEL은 이번 로그추적기를 만들면서 하위의 결과처럼 |--> 등과 같이 컨트롤러는 LEVEL이 0이여서 아무것도 출력하지 않았지만 두번째 서비스는 LEVEL이 1이여서 |-->를 출력했다. 이전에는 LEVEL에 대해서 값을 지정을 해주지 않아서 0으로 유지되었으며 |-->가 뜨지 않는게 당연했다.

*[af3ea3e9]  request OrderControllerV2*
*[af3ea3e9] |--> request OrderServiceV2*
*[af3ea3e9] |    |--> request OrderRepositoryV2*
*[af3ea3e9] |    |<-- request OrderRepositoryV2 time=1005ms*
*[af3ea3e9] |<-- request OrderServiceV2 time=1006ms*
*[af3ea3e9]  request OrderControllerV2 time=1006ms*



우선 V2의 컨트롤러는 위의 코드와 같다.

하지만 바뀐점이 있다면 TRACEID 객체에 밑의 메서드가 추가되었다는 것이다. 

```java
public TraceId createNextId(){
    return new TraceId(id,level+1);
}
```

이는 begin이 생성해낸 status객체 안에 정보를 level과 id정보를 담고있는 traceid객체 안에 createNextId메서드를 실행하면 리턴해주는 값은 id값을 유지하며 level정보는 증가시켜서 값을 추출해낼 수 있다.

```java
public TraceStatus beginSync(TraceId beforeTraceId,String message){
    TraceId traceId = beforeTraceId.createNextId();

    Long startTimeMs = System.currentTimeMillis();
    log.info("[{}] {} {}", traceId.getId(),
             addSpace(START_PREFIX, traceId.getLevel()),
             message);
    
    return new TraceStatus(traceId, startTimeMs, message);
}
```

그리고 로그추적기에 beginSync 동길화를 해줄 수 있는 메서드를 만들었다. 이 메서드는 begin에서 생성한 status객체에서 traceId를 빼내어 인자로 주게되면 id는 유지하되 level의 값을 증가시키고, 그 정보로 로그를 출력한 후 status에 다시 재포장 시켜서 넘겨주게 된다.

```java

@Service
@RequiredArgsConstructor
public class OrderServiceV2 {

    private final OrderRepositoryV2 orderRepositoryV2;
    private final HelloTraceV2 helloTraceV2;

    public void orderItem(TraceId traceId, String itemId){
        TraceStatus status = null;
        try{
            status = helloTraceV2.beginSync(traceId,"request OrderServiceV2");
            orderRepositoryV2.save(status.getTraceId(),itemId);
            helloTraceV2.end(status);
        }catch (Exception e){
            helloTraceV2.exception(status, e);
            throw e; 
        }
    }

}
```

@service 단의 메서드이다. 컨트롤러는 변경이 없었지만 traceID를 넘겨받기 위해서 인자가 하나 더 추가된 것이 보인다. ` public void save(TraceId traceId, String itemId)` 이렇게 인자를 넘겨받아 메세지와 traceID를 직접 beginSync에 넘겨주게 되면 새로운 레벨값만 바뀐 status가 넘어오게 된다. 

마지막으로 앞에서 리뷰를 하지 못한 addSpace 메서드인데

```java
private static String addSpace(String prefix, int level){
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i<level; i++){
        sb.append((i==level-1)?"|"+prefix:"|    ");
    }
    return sb.toString();
}
```

레벨에 따라서 buffer에 string을 추가해서 그것을 출력해서 로그에 뿌려준다.

```tex
    String addSpace(String prefix, int level) : 다음과 같은 결과를 출력한다.
    
    prefix: -->
    level 0: 
    level 1: |-->  
    level 2: | |-->
    
    prefix: <--
    level 0:
    level 1: |<--
    level 2: | |<--
    
    prefix: <X-level 0:
    level 1: |<X-
    level 2: | |<X-
```



## ThreadLocal

<hr>

> 위의 예제에서는 컨트롤러에서 traceID 의 (트랜잭션) 유지를 위해서 인자로 일일이 traceID의 값을 넘겨주면서 sync메서드를 통해서 createNextId메서드를 통해 traceId를 얻어왔다.
>
> 하지만 이렇게 계속 컨트롤러 인자에 영향을 주면서 넘겨야 할까?

그래서 기존에 소스코드에 `private TraceId traceIdHolder;` 의 필드를 추가를 해주었다.

기존의 beginSync 메서드를 통한 동기화 말고 필드에 저장하고 동기화를 택한 것이다.

beginSync 메서드는 사라지고 필드가 하나 생겼으며 메서드는 두개가 추가 되었다.

```java
private void syncTraceId(){
    if(traceIdHolder == null){
        traceIdHolder = new TraceId();           
    }else{
        traceIdHolder = traceIdHolder.createNextId
    }
}
.
.
.
private void releaseTraceId() {
        if(traceIdHolder.isFirstLevel()){
            traceIdHolder = null;//destroy
        }else{
            traceIdHolder = traceIdHolder.createPreviouslyId();
        }
    }
}
```

`syncTraceId()`에서는 traceHolder 필드가 null로 비어있으면 첫 번째 트랜잭션의 로그라고 생각해서 생성자로 ID를 생성하고 LEVEL이 0인 traceID를 생성하게 된다.

```java
@Override
public TraceStatus begin(String message) {
    syncTraceId();
    TraceId traceId = traceIdHolder;
    Long startTimeMs = System.currentTimeMillis();
    log.info("[{}] {} {}",
             traceId.getId(),
             addSpace(START_PREFIX, traceId.getLevel()),
             message);
    return new TraceStatus(traceId, startTimeMs, message);
}
```

이런식으로 필드에 계속 동기화를 맞춰주게 되면 인자로 traceID를 받을 일도 없을 것이다.

```java
private void complete(TraceStatus status, Exception e) {
    Long stopTime = System.currentTimeMillis();
    long resultTimeMs = stopTime - status.getStartTimeMs();
    TraceId traceId = status.getTraceId();
	.
    .
    .
    releaseTraceId();
}
```

` releaseTraceId();` 또 이메서드가 하나 추가되었다.

```java
private void releaseTraceId() {
    if(traceIdHolder.isFirstLevel()){
        traceIdHolder = null;
    }else{
        traceIdHolder = traceIdHolder.createPreviouslyId();
    }
}

//TraceID
public boolean isFirstLevel(){
    return level == 0;
}
```

인자로 traceID를  받지 않고 컨트롤러에서 traceID가 유지되는 것이 아니라 필드에 유지 되어지는 것이기 때문에 LEVEL 체크를 통해서 마지막 로그이게 되면 `null` 상태로 변환시켜주게 되고 그게 아닐시 `createPreviouslyId()` 를 통해서 `id`는 유지하되 레벨을 `level - 1` 을 해주고 traceID를 반환받게 된다.

기존 코드에서 (`package hello.advanced.app.v3;`)이렇게 필드에서 동기화를 수행하는게 가능해진다.



#### 동시성 문제

> ✍️ 하지만 이렇게 필드로 유지를 한다는 것은 정말 위험한 방법이다.
>
> 동시성 문제를 배재할 수 없다. 스프링은 항상 싱글톤을 베이스로 빈으로 등록되어지기 때문에 필드로 상태유지를 할 시에 다른 스레드가 접근하면 그 값을 똑같이 참조한다. 즉, 다른 스레드라도 트랜잭션 구분이 되어지지 않는다는 뜻이다.



[nio-8080-exec-5] h.a.app.trace.logtrace.FieldLogTrace     : [e687ce3b] |--> MESSAGE
[nio-8080-exec-5] h.a.app.trace.logtrace.FieldLogTrace     : [e687ce3b] |    |--> MESSAGE
[nio-8080-exec-6] h.a.app.trace.logtrace.FieldLogTrace     : [e687ce3b] |    |    |--> MESSAGE
[nio-8080-exec-6] h.a.app.trace.logtrace.FieldLogTrace     : [e687ce3b] |    |    |    |--> MESSAGE
[nio-8080-exec-6] h.a.app.trace.logtrace.FieldLogTrace     : [e687ce3b] |    |    |    |    |--> MESSAGE
[nio-8080-exec-7] h.a.app.trace.logtrace.FieldLogTrace     : [e687ce3b] |    |    |    |    |    |--> MESSAGE



실제로 서버를 돌려놓고 요청을 컨트롤러에 동시에 주게 되면 다음과 같이 레벨이 깊게 파이고 트랜잭션 아이디도 구분이 되지 않는다. 여기서 트랜잭션 아이디를 구분하는 방법은 `[nio-8080-exec-5]`  가 톰캣에서 기본적으로 제공하는 스레드의 이름이다. 5,6,7 번이 동시에 요청을 했지만 필드가 서로 스레드간에 공유되어지고 있기 때문에 어쩔 수 없다.



#### 동시성 문제의 자세한 고찰

> 이 동시성 문제에 대한 기본적인 학습을 위한 코드를 테스트 코드에 작성을 했습니다. `package hello.advanced.trace.threadlocal;` 에서 다음의 코드는 밑의 코드와 같다.



밑의 `FieldService` 클래스는 동시성 문제가 생기는 코드이다. 일단 `namestore` 필드를 서로 스레드에 공유해주기 위해서 만들었고 로직에는 인자로 넘어온`String` 값이 해당 공유변수에 들어가게 된다.

그리고 가정하기 위해 저장하는데 1초가 걸린다고 전제를 깔았다.

```java
@Slf4j
public class FieldService {
    private String nameStore;

    public String logic(String name){
        log.info("저장 name={} -> nameStore={}",name,nameStore);
        nameStore = name;
        sleep(1000);
        log.info("조회 nameStore={}",nameStore);
        return nameStore;
    }
```

그리고 밑의 코드는 스레드를 이용해서 동시성 오류를 내보았다.

```java
@Slf4j
public class FieldServiceTest {
    private FieldService fieldService = new FieldService();

    @Test
    public void field() {
        Runnable userA = ()-> {
            fieldService.logic("userA");
        };
        Runnable userB = ()-> {
            fieldService.logic("userB");
        };

        Thread threadA = new Thread(userA);
        threadA.setName("thread-A");
        Thread threadB = new Thread(userB);
        threadB.setName("thread-B");

        threadA.start();
        sleep(100);
        threadB.start();
        sleep(3000);
        //SLEEP 메서드는 코드 축약을 위해 본 코드에서 잘라냄.
    }
}
```
*저장 name=userA -> nameStore=null*
*저장 name=userB -> nameStore=userA*
*조회 nameStore=userB*
*조회 nameStore=userB*

🧨중간에 A스레드가 1초를 넘기지 못하고 메인스레드가 당장 다음 스레드인 B스레드를 호출했을 때, B는 그 A가 저장되어있던 공유 변수를 중간에 탈취해서 자신의 `userB` 값을 저장하게 된다. 

그러면 A스레드는 분명 `userA`를 저장하였지만 조회할 때는 `userB`를 조회하는 것이다.

우리는 그래서 동시성 문제를 회피하기 위해서 자바가 기본적으로 제공하는 `java.lang.threadLocal` 을 사용한다.



#### ThreadLocal 이란?

<hr>

>  스레드 로컬은 스레드 끼리 동시성 문제를 막고자 나온 클래스이다.
>
> 예를 들면 a라는 사람과 b라는 사람이 같은 창고를 쓰는데 ab라는 물건을 찾아야 한다. 둘다 ab를 예약했다고 해서 같은 시간에 똑같은 ab를 가질 수는 없다. 그래서 ab(1), ab(2) 수요에 맞춰서 보관하고 그에 맞춰서 1은 a에게 2는 b에게 맞춰서 줄 수 있다.

```java
public class ThreadLocalService {
    private ThreadLocal<String> nameStore = new ThreadLocal<>();
    public String logic(String name){
        log.info("저장 name={} -> nameStore={}",name,nameStore.get());
        nameStore.set(name);
        sleep(100);
        log.info("조회 nameStore={}",nameStore.get());
        return nameStore.get();
    }
}
```

`private ThreadLocal<String> nameStore = new ThreadLocal<>();` 로 바꾸었고 달라진 점이 하나 있다면, `nameStore.get()` ,`nameStore.set(<Generic Type>)` ,`nameStore.remove()` 를 사용한다.

저상태로 바꿔주고 위에서 테스트 코드 중 동시성 에러를 띄우는 `sleep(100);` 쪽도 상관이 없어지고 테스트코드를 돌리게 되면 결과 값은,

*저장 name=userA -> nameStore=null*
*조회 nameStore=userA*
*저장 name=userB -> nameStore=null*
*조회 nameStore=userB*

이런식으로 첫번째 nameStore에는 null 이고 그 다음은 b여야 하는데 각각의 다른 저장소 필드를 사용하게 되어서 동시성 이슈가 해결이 되었다.

<hr>

이런 점을 V3 코드에 적용시켜 보았다.

```java
@Slf4j
@Component
public class ThreadLocalLogTrace implements LogTrace {

    private static final String START_PREFIX = "-->";
    private static final String COMPLETE_PREFIX = "<--";
    private static final String EX_PREFIX = "<X-";

    private ThreadLocal<TraceId> traceIdHolder = new ThreadLocal<>();
    private void syncTraceId(){
        TraceId traceId = traceIdHolder.get();
        if(traceId == null){
            traceIdHolder.set(new TraceId());
        }else{
            traceIdHolder.set(traceId.createNextId());
        }
    }

    @Override
    public TraceStatus begin(String message) {
      
        syncTraceId();
        TraceId traceId = traceIdHolder.get();
        Long startTimeMs = System.currentTimeMillis();
        log.info("[{}] {} {}", traceId.getId(),
                 addSpace(START_PREFIX, traceId.getLevel()),
                 message);
        return new TraceStatus(traceId, startTimeMs, message);
    }

    @Override
    public void end(TraceStatus status) {
        complete(status,null);
    }

    @Override
    public void exception(TraceStatus status, Exception e) {
        complete(status,e);
    }

    private void complete(TraceStatus status, Exception e) {
        Long stopTime = System.currentTimeMillis();
        long resultTimeMs = stopTime - status.getStartTimeMs();
        TraceId traceId = status.getTraceId();

        if(e == null){
            log.info("[{}] {} {} time={}ms",status.getTraceId().getId(),
                     addSpace(COMPLETE_PREFIX,traceId.getLevel()),
                     status.getMessage(),
                     resultTimeMs);
        }else{
            log.info("[{}] {} {} ex={}",
                     status.getTraceId().getId(),addSpace(EX_PREFIX,traceId.getLevel()),
                     status.getMessage(), e.toString());
        }

        releaseTraceId();
    }

    //public boolean isFirstLevel(){
    //    return level == 0;
    //}
    private void releaseTraceId() {
        TraceId traceId = traceIdHolder.get();
        if(traceId.isFirstLevel()){
            traceIdHolder.remove();//destroy
        }else{
            traceIdHolder.set(traceId.createPreviouslyId());
        }
        
    }

    private static String addSpace(String prefix, int level){
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i<level; i++){
            sb.append((i==level-1)?"|"+prefix:"|    ");
        }
        return sb.toString();
    }
}
```

traceID를 동시성 문제를 해결하기 위해 ThreadLocal 클래스에 집어넣었다.

기존 코드에서 바뀔 필요 없이 해당 traceID를 쓰는 과정만 set, remove, get을 이용하여 사용해주자.



> ![동시성_문제](동시성_문제.PNG)
>
> 🧨 remove를 사용해주어야 하는 이유는 was에서 요청이 들어왔을 때 스레드 풀에서 미리 스레드를 만들어 놓고 해당 스레드에게 접속을 허용한다. 그리고 그 스레드가 나갔을 때 스레드를 풀에 반환해서 다시 다음 사용자가 사용할 수 있도록 반환하도록 한다. 
>
> 하지만 스레드가 나오는 건 랜덤이라서 (재사용의 관점) 전에 쓰던 사용자의 스레드를 다른 사용자가 받게 되면 그 해당 스레드로컬을 조회할 때 전에 쓰던 사용자의 데이터를 넘겨 받을 수 있는 노릇이다.
>
> 그렇기 때문에 스레드의 작업이 끝나게 되면 remove로 스레드 로컬을 초기화 시켜주도록 하자.



#### 템플릿 메서드 패턴

<hr>

> 위에서 짰던 로그추적기의 코드는 핵심(종단 관심) 로직과 부가적인 코드(횡단 관심)에 대해서 분리가 전혀 되지 않는다. 
>
> ```java
> TraceStatus status = null;
> try{
>     status = logTrace.begin("request OrderControllerV3");
>     orderServiceV3.orderItem(itemId);
>     logTrace.end(status);
>     return "ok";
> }catch (Exception e){
>     logTrace.exception(status, e);
>     throw e;
> }
> ```
>
> 기존의 코드는 분리가 되지 않은 코드다 중복 되는 횡단관심의 코드들을 다 뽑아서 리팩토링해서 한번에 쓸 수 있다면 좋을 것 같지만 `try catch` 문 안에 있기도 하고 위아래로 `end,exception`, `begin` 이 핵심을 위아래로 감싸고 있다.
>
> 이를 해결할 수 있는 방법이 템플릿 메서드 패턴이다.

```java
@Test
void templateMethodV0(){
    logic1();
    logic2();
}
```

```java
private void logic1(){
    long startTime = System.currentTimeMillis();
    log.info("비지니스 로직1 실행");
    long endTime = System.currentTimeMillis();
    long resultTime = endTime - startTime;
    log.info("resultTime = {}",resultTime);
}

private void logic2(){
    long startTime = System.currentTimeMillis();
    log.info("비지니스 로직2 실행");
    long endTime = System.currentTimeMillis();
    long resultTime = endTime - startTime;
    log.info("resultTime = {}",resultTime);
}
```

시간을 출력하는 메서드를 제외하고 핵심 로직만 다른 상태를 유지하는 테스트코드를 만들었다.

이제 이 소스코드를 템플릿 메서드 패턴으로 변환 시켜서 실행 시켜보겠다.

```java
@Slf4j
public abstract class AbstractTemplate {
    public void execute(){
        long startTime = System.currentTimeMillis();
        call();
        long endTime = System.currentTimeMillis();
        long resultTime = endTime - startTime;
        log.info("resultTime = {}",resultTime);
    }
    protected abstract void call();
}
```

템플릿 메서드 패턴은 부모 클래스와 자식 클래스 간의 상속구조와 오버라이딩을 이용해서 풀어내는 패턴을 말한다. 전체적인 `AbstractTemplate` 클래스에서 `execute`에 공통 로직(횡단 관심) 을 짜주고 그 밑에 오버라이딩 할 수 있게 추상메서드로 `call` 메서드를 만들어준다. 그리고 횡단 관심이 적혀있는 로직에 핵심로직이 들어가야 할 부분에 call을 적어준다. 

여기서 `call()` 메서드는 핵심로직을 구현하기 위해 만들어주는 추상메서드이다. 이것을 이제 클래스로 구현해서 클래스를 상속받고 `call`을 구현해줄 것이다.

```java
@Slf4j
public class SubClassLogic1 extends AbstractTemplate{
    @Override
    protected void call() {
        log.info("비즈니스 로직1 실행");
    }
}
```

따로 클래스를 만들어 상속 받고 call 을 오버라이드 한 상태이다.

이렇게 되면 자바 상속 구조 상 부모에서 call을 실행하게 되면 그 자식의 오버라이드 메서드를 자동으로 실행한다.

```java
AbstractTemplate template1 = new SubClassLogic1();
template1.execute();
```

사용은 이렇게 한다. 이렇게 만들어주어도 되지만

```java
@Test
private void templateMethodV2(){
    AbstractTemplate template1 = new AbstractTemplate(){
        @Override
        protected void call() {
            log.info("비지니스 로직1 실행");
        }
    };
    template1.execute();
}
```

익명클래스를 이용해서 하는 것도 클래스를 만들지 않고 일회성으로 사용하기 좋은 방법이다.



##### 로그 추적기에 적용해보기

<hr>

```java
public abstract class AbstractTemplate<T> {
    private final LogTrace trace;

    public AbstractTemplate(LogTrace trace){
        this.trace = trace;
    }

    public T execute(String message){
        TraceStatus status = null;
        try {
            status = trace.begin(message);
            T result = call();
            trace.end(status);

            return result;
        }catch (Exception e){
            trace.exception(status,e);
            throw e;
        }
    }

    protected abstract T call();
}
```

로그 추적기를 템플릿 메서드 패턴으로 구현해주기 위해서는 이전 메서드와 같이 execute를 정의해주는데 반환 타입은 T인 제네릭클래스로 받았다.

Service Repository Controller 단에서 각자 반환되는 형태가 달라서 제네릭으로 일단 설정했다.

`protected abstract T call();` 이제 이 메서드를 구현해줄 차례인데 이너클래스로 적도록 하겠다.

```java
@RestController
@RequiredArgsConstructor
public class OrderControllerV4 {

    private final OrderServiceV4 orderServiceV4;
    private final LogTrace trace;

    @GetMapping("/v4/request")
    public String request(String itemId){

        AbstractTemplate<String> template = new AbstractTemplate<String>(trace) {
            @Override
            protected String call() {
                orderServiceV4.orderItem(itemId);
                return "ok";
            }
        };
        return template.execute("OrderController request V4");
    }
}
```

확실히 `try/catch`문을 쓸 때 보다 정말 많이 간편해졌다.

inner클래스로 인해서 많이 깔끔해지긴 했다.

#### template method pattern 의 단점

<hr>

템플릿 메서드 패턴 자체가 단일책임 원칙을 지키면서 전체적인 구조를 변경하지 않고 특정 부분만 수정을해서 사용할 수 있다는 장점이 있지만 템플릿 메서드 패턴은 <b>상속</b>을 사용한다.

이게 문제가 무엇이냐면 의존관계에 문제가 있다. 자식은 부모의 속성을 전혀 사용하지 않았는데 자식은 부모의 속성을 다 알고 있다. 그리고 자식이 부모를 향하고 있는 상속의 구조는 부모에 만약에 call 말고 call2 추상메서드가 정의 되었다고 하면 하나하나 자식클래스에다가 대고 오버라이드해주어야한다.

이는 정말 좋은 설계가 아니다.

부모를 강하게 의존하고 있다는 단점을 커번 패턴이 전략 패턴이다.

이번엔 전략 패턴에 대해서 알아보도록 하겠다.



### 전략 패턴

<hr>





