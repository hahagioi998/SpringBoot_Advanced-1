package hello.advanced.trace.template.code;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SubClassLogic1 extends AbstractTemplate{

    @Override
    protected void call() {
        log.info("비즈니스 로직1 실행");
    }
    /*
     부모클래스에서 call2 가생겼다고 치면 자식클래스는 일일 이 구현해야한다.
     */
}
