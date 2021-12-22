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



[8fe7bbff]  request OrderControllerV1
[8251cfd5]  request OrderServiceV1
[17348592]  request OrderRepositoryV1
[17348592]  request OrderRepositoryV1 time=1007ms
[8251cfd5]  request OrderServiceV1 time=1007ms
[8fe7bbff]  request OrderControllerV1 time=1008ms

의 결과가 나오게 되는데 service와 repository는 `package hello.advanced.app.v1;` 의 패키지 안에서 보도록 하자.

> 여기서 http하나의 요청에 대해 트랜잭션 id값도 다르고 깊이도 출력하지 않는다. 다음 v2에 대해서는 깊이와 id값의 유지를 해볼 것이다.
>
> 위사항을 해결하기 위해서는 로그에 대한 문맥(contex)정보를 가져와서 이전에 어떤 트랜잭션을 썼고 level이 뭐였는지를 전달해야한다.



##### 로그추적기V2

<hr>

기존 traceID를 유지하고 LEVEL의 정보를 추가하여 깔끔하게 볼 수 있도록 하기 위해 코드를 좀 더 수정해 보았다.

✍️ 여기서 LEVEL은 이번 로그추적기를 만들면서 하위의 결과처럼 |--> 등과 같이 컨트롤러는 LEVEL이 0이여서 아무것도 출력하지 않았지만 두번째 서비스는 LEVEL이 1이여서 |-->를 출력했다. 이전에는 LEVEL에 대해서 값을 지정을 해주지 않아서 0으로 유지되었으며 |-->가 뜨지 않는게 당연했다.

[af3ea3e9]  request OrderControllerV2
[af3ea3e9] |--> request OrderServiceV2
[af3ea3e9] |    |--> request OrderRepositoryV2
[af3ea3e9] |    |<-- request OrderRepositoryV2 time=1005ms
[af3ea3e9] |<-- request OrderServiceV2 time=1006ms
[af3ea3e9]  request OrderControllerV2 time=1006ms



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

