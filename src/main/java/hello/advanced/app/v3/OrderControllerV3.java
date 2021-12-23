package hello.advanced.app.v3;

import hello.advanced.app.trace.TraceStatus;
import hello.advanced.app.trace.hellotrace.HelloTraceV2;
import hello.advanced.app.trace.logtrace.FieldLogTrace;
import hello.advanced.app.trace.logtrace.LogTrace;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderControllerV3 {

    private final OrderServiceV3 orderServiceV3;
    private final LogTrace logTrace;

    @GetMapping("/v3/request")
    public String request(String itemId){
        TraceStatus status = null;
        try{
            status = logTrace.begin("request OrderControllerV3");
            orderServiceV3.orderItem(itemId);
            logTrace.end(status);
            return "ok";
        }catch (Exception e){
            logTrace.exception(status, e);
            throw e;
        }
        /*
             로그 추적기를 완성했으면 항상
             문제가 생기기 마련이다. 이런식으로 해줘도
             상태저장 문제때문에 (싱글톤) 항상 동시성 이슈가 생기기 마련인데
             면접질문에서도 엄청많이 나온다는 얘기입니다.

             동시성이슈를 확인하기 위해서는 1초에 2번의 request를 보내보자.

             2021-12-23 14:55:21.419  INFO 15052 --- [nio-8080-exec-5] h.a.app.trace.logtrace.FieldLogTrace     : [e687ce3b]  request OrderControllerV3
            2021-12-23 14:55:21.419  INFO 15052 --- [nio-8080-exec-5] h.a.app.trace.logtrace.FieldLogTrace     : [e687ce3b] |--> request OrderServiceV3
            2021-12-23 14:55:21.419  INFO 15052 --- [nio-8080-exec-5] h.a.app.trace.logtrace.FieldLogTrace     : [e687ce3b] |    |--> request OrderRepositoryV3
            2021-12-23 14:55:21.653  INFO 15052 --- [nio-8080-exec-6] h.a.app.trace.logtrace.FieldLogTrace     : [e687ce3b] |    |    |--> request OrderControllerV3
            2021-12-23 14:55:21.653  INFO 15052 --- [nio-8080-exec-6] h.a.app.trace.logtrace.FieldLogTrace     : [e687ce3b] |    |    |    |--> request OrderServiceV3
            2021-12-23 14:55:21.653  INFO 15052 --- [nio-8080-exec-6] h.a.app.trace.logtrace.FieldLogTrace     : [e687ce3b] |    |    |    |    |--> request OrderRepositoryV3
            2021-12-23 14:55:22.176  INFO 15052 --- [nio-8080-exec-7] h.a.app.trace.logtrace.FieldLogTrace     : [e687ce3b] |    |    |    |    |    |--> request OrderControllerV3
            2021-12-23 14:55:22.176  INFO 15052 --- [nio-8080-exec-7] h.a.app.trace.logtrace.FieldLogTrace     : [e687ce3b] |    |    |    |    |    |    |--> request OrderServiceV3
            2021-12-23 14:55:22.177  INFO 15052 --- [nio-8080-exec-7] h.a.app.trace.logtrace.FieldLogTrace     : [e687ce3b] |    |    |    |    |    |    |    |--> request OrderRepositoryV3
            2021-12-23 14:55:22.425  INFO 15052 --- [nio-8080-exec-5] h.a.app.trace.logtrace.FieldLogTrace     : [e687ce3b] |    |<-- request OrderRepositoryV3 time=1006ms
            2021-12-23 14:55:22.425  INFO 15052 --- [nio-8080-exec-5] h.a.app.trace.logtrace.FieldLogTrace     : [e687ce3b] |<-- request OrderServiceV3 time=1006ms
            2021-12-23 14:55:22.425  INFO 15052 --- [nio-8080-exec-5] h.a.app.trace.logtrace.FieldLogTrace     : [e687ce3b]  request OrderControllerV3 time=1006ms

        [nio-8080-exec-5]
        톰캣이 제공하는 스레드 번호이다. 5번... -> 7번스레드... 6번스레드...

        스레드별로 구분하면
        트랜잭션 자체가 구분도 안되고 뭔 이상하게 레벨도 개무시당하고 있다.

        스프링은 싱글톤으로 된 스프링 빈이다. 이객체의 인스턴스가 어플리케이션 자체에 하나만 딱 존재한다는 뜻입니다. 이렇게 하나만 있는
        인스턴스의 fieldlogtrace의 traceIdHolder 필드를 여러 스레드가 동시에 접근하기 때문에 문제가 발생합니다.
        실무에서 한번 나타나면 개발자를 가장 괴롭히는 문제도 바로 이러한 동시성문제입니다.
        */
    }
}
