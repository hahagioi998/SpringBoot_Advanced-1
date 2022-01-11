package hello.advanced.trace.strategy;

import hello.advanced.trace.strategy.code.template.Callback;
import hello.advanced.trace.strategy.code.template.TimeLogTemplate;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class TemplateCallbackTest {
    /*
     * 템플릿 콜백 패턴 - 익명 내부 클래스
     */
    @Test
    void callbackV1(){
        TimeLogTemplate timeLogTemplate = new TimeLogTemplate();
        timeLogTemplate.execute(new Callback() {
            @Override
            public void call() {
                log.info("템플릿 콜백 패턴V1(1) 로직 실행");
            }
        });

        timeLogTemplate.execute(()->log.info("템플릿 콜백 패턴V1(2) 로직 실행"));
        /*
         * 별도의 클래스를 만들어서 구현체로 넣어줘도 되지만 인터페이스 하나만 가지고 구현해도 상관없다.
         */
    }
}
