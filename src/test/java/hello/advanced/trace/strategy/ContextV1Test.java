package hello.advanced.trace.strategy;

import hello.advanced.trace.strategy.code.strategy.ContextV1;
import hello.advanced.trace.strategy.code.strategy.Strategy;
import hello.advanced.trace.strategy.code.strategy.StrategyLogic1;
import hello.advanced.trace.strategy.code.strategy.StrategyLogic2;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import javax.naming.Context;

@Slf4j
public class ContextV1Test {

    @Test
    void templateMethodV0(){
        logic1();
        logic2();
    }

    private void logic1(){
        long startTime = System.currentTimeMillis();
        //비지니스 로직 실행
        log.info("비지니스 로직1 실행");
        //비지니스 로직 종료
        long endTime = System.currentTimeMillis();
        long resultTime = endTime - startTime;
        log.info("resultTime = {}",resultTime);
    }

    private void logic2(){
        long startTime = System.currentTimeMillis();
        //비지니스 로직 실행
        log.info("비지니스 로직2 실행");
        //비지니스 로직 종료
        long endTime = System.currentTimeMillis();
        long resultTime = endTime - startTime;
        log.info("resultTime = {}",resultTime);
    }

    @Test
    void 전략패턴V1(){
        StrategyLogic1 strategyLogic1 = new StrategyLogic1();
        ContextV1 context1 = new ContextV1(strategyLogic1);
        context1.execute();
        //컨텍스트 안에 내가 원하는 전략패턴의 구현체를 넣어서 컨텍스트를 실행한다.
        //그리고나서 execute로 실행을 해주면 컨텍스트 로직을 실행하고 실행결과를 출력.

        StrategyLogic2 strategyLogic2 = new StrategyLogic2();
        ContextV1 context2 = new ContextV1(strategyLogic2);
        context2.execute();
        /*
        23:27:59.480 [main] INFO hello.advanced.trace.strategy.code.strategy.StrategyLogic1 - 비지니스 로직1 실행
        23:27:59.482 [main] INFO hello.advanced.trace.strategy.code.strategy.ContextV1 - resultTime = 3
        23:27:59.483 [main] INFO hello.advanced.trace.strategy.code.strategy.StrategyLogic2 - 비지니스 로직2 실행
        23:27:59.483 [main] INFO hello.advanced.trace.strategy.code.strategy.ContextV1 - resultTime = 0
         */
    }

    @Test
    void 전략패턴V2_익명내부클래스(){
        ContextV1 context = new ContextV1(new Strategy() {
            @Override
            public void call() {
                log.info("전략패턴의 익명 내부 클래스 구현");
            }
        });
        context.execute();
        // 물론 템플릿 메서드 패턴 처럼 익명내부클래스로 구현이 가능하기도 하다. 하지만 피하자.
        //$2 $1 getClass 해서 이렇게 이름 나온게 익명 이너 클래스임.
    }
}
