package hello.advanced.trace.strategy.code.strategy;

import lombok.extern.slf4j.Slf4j;


/*
 밑에 생성자와 필드가 주석처리 된 것을 볼 수 있다.
 이번엔 필드에 전략을 가지고 있는 것이 아닌 execute 실행시점에 바로 받아 볼 수 있도록 하는 패턴을 적용하도록 하겠다.
 */

@Slf4j
public class ContextV2 {
    /*private Strategy strategy;

    public ContextV2(Strategy strategy){
        this.strategy = strategy;
    }*/

    public void execute(Strategy strategy){
        long startTime = System.currentTimeMillis();
        //비지니스 로직 실행
        strategy.call(); // 위임
        //비지니스 로직 종료
        long endTime = System.currentTimeMillis();
        long resultTime = endTime - startTime;
        log.info("resultTime = {}",resultTime);
    }
}
