package hello.advanced.trace.template.code;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SubClassLogic2 extends AbstractTemplate{

    @Override
    protected void call() {
        log.info("비즈니스 로직2 실행");
    }
    //변하는 부분에 대한 자식클래스를 생성한다. call 오버라이딩.
}
