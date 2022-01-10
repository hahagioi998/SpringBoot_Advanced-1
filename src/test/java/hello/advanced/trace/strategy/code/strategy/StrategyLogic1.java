package hello.advanced.trace.strategy.code.strategy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StrategyLogic1 implements Strategy{
    @Override
    public void call() {
        log.info("비지니스 로직1 실행");
    }
    //변하는 알고리즘은 이렇게 1과 2로 나누어서 구현할 예정.
}
