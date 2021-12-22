package hello.advanced.app.v2;

import hello.advanced.app.trace.TraceId;
import hello.advanced.app.trace.TraceStatus;
import hello.advanced.app.trace.hellotrace.HelloTraceV1;
import hello.advanced.app.trace.hellotrace.HelloTraceV2;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryV2 {

    private final HelloTraceV2 helloTraceV2;

    //상품을 저장하는 로직
    public void save(TraceId traceId, String itemId){

        TraceStatus status = null;
        try{
            status = helloTraceV2.beginSync(traceId,"request OrderRepositoryV2");

            if(itemId.equals("ex")){
                //상품이름이 ex이면 에러가난다.
                throw new IllegalStateException("예외 발생!");
            }
            sleep(1000);

            helloTraceV2.end(status);
        }catch (Exception e){
            helloTraceV2.exception(status, e);
            throw e; //예외를 꼭 다시 던져주어야 한다.
            // 중요한점은 어플리케이션의 흐름을 내가 직접 바꾸어서는 안된다.
            // 예외가 발생하면 꼭 다시 던져주도록 하자. (정상흐름으로 바뀌기 때문에 에러가 의미가 없어진다.)
        }
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }
}
