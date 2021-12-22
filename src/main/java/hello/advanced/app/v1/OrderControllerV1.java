package hello.advanced.app.v1;

import hello.advanced.app.trace.TraceStatus;
import hello.advanced.app.trace.hellotrace.HelloTraceV1;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderControllerV1 {

    private final OrderServiceV1 orderServiceV1;
    private final HelloTraceV1 helloTraceV1;

    @GetMapping("/v1/request")
    public String request(String itemId){
        TraceStatus status = null;
        try{
            status = helloTraceV1.begin("request OrderControllerV1");
            orderServiceV1.orderItem(itemId);
            helloTraceV1.end(status);
            return "ok";
        }catch (Exception e){
            helloTraceV1.exception(status, e);
            throw e; //예외를 꼭 다시 던져주어야 한다.
                    // 중요한점은 어플리케이션의 흐름을 내가 직접 바꾸어서는 안된다.
                    // 예외가 발생하면 꼭 다시 던져주도록 하자. (정상흐름으로 바뀌기 때문에 에러가 의미가 없어진다.)
        }
    }

//[8fe7bbff]  request OrderControllerV1
//[8251cfd5]  request OrderServiceV1
//[17348592]  request OrderRepositoryV1
//[17348592]  request OrderRepositoryV1 time=1007ms
//[8251cfd5]  request OrderServiceV1 time=1007ms
//[8fe7bbff]  request OrderControllerV1 time=1008ms

    //이런식으로 로그가 뜬다.
    //현재는 이방식이 맞는 것이고 뒤에서 다시 레벨과 트랜잭션 id를 다루자.
    //지금은 begin 에서 TraceId traceId = new TraceId() 바로 컨트롤러,서비스,레포단까지 traceid를 각각 생성해내고
    //그에 따른 트랜잭션 id값을 따로 쓰고 있다, 또 level 값을 아예 건드리지를 않아서  primitive 타입인 0을 가지고 있어서 레벨이 출력되지 않는다.
    //http요청에 대해서 구분을 하지 못하고 지금 메서드 호출 깊이 x

    //위사항을 해결하기 위해서는 로그에 대한 문맥(contex)정보를 가져와서 이전에 어떤 트랜잭션을 썼고 level이 뭐였는지를 전달해야한다.
}
