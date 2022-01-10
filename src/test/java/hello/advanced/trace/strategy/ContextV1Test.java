package hello.advanced.trace.strategy;

import hello.advanced.trace.strategy.code.strategy.*;
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

    @Test
    void 전략패턴V2_익명내부클래스_람다(){
        ContextV1 context = new ContextV1(()->{log.info("비지니스 로직 람다 구현");});
        /*
         단, 람다를 구현하기 위해서는 인터페이스가 한개만 존재하여야 한다.

         뭔가 하지만 선 조립 후 실행 이라는 문장을 연상케 한다.
         이것보다 더 간편하게 쓸 수 있는 방법이 없을까?

         이 방식의 단점은 context와 strategy를 조립 후에 내부로 구현한 익명클래스를 변경하기가 어렵다는 점이다.
         또, context에 setter를 제공해서 strategy를 넘겨 받아 변경하면 되지만 context를 싱글톤으로 사용할 때는 동시성 이슈 등
         고려할 점이 많다. 그래서 전략을 실시간으로 변경해야하면 차라리 이전에 개발한 테스트 코드처럼 context를 하나 더 생성하고
         아예 다시 조립해서 실행하는 것이 더 나은 선택일 수 있다.
         */
        context.execute();


    }


    /*
     * ref. contextV2
     */
    @Test
    void 전략패턴V2_조립시점이_아닌_컨텍스트에_인자로_컨텍스트_실행시점에(){
        ContextV2 contextV2 = new ContextV2();
        contextV2.execute(new StrategyLogic1());
        contextV2.execute(new StrategyLogic2());
        //그때 그때 인자로 받아서 실행시키는 시점에 원하는 로직을 집어넣고 원하는 시점에 실행하며 자유롭게
        // 더 유연하게 바꿀 수 있는 장점이 있다.

        contextV2.execute(new Strategy() {
            @Override
            public void call() {
                log.info("비지니스 로직1 실행");
            }
        });

        contextV2.execute(() -> log.info("비지니스 로직2 실행"));
        //변하는 부분과 변하지 않는 부분에 대해서 분리해내는 것이다.
        //변하지 않는 부분을 템플릿이라고 하고, 그 템플릿 안에서 변하는 부분에 약간 다른 코드 조각을 넘겨서 실행하는 것이
        //목적이다.
        //두가지 방식 다 문제를 해결할 수 있지만 어떤 방식이 더 나아보이냐면 실행할 때 변하지 않는 템플릿이 있고
        //그 부분에 대해서만 살짝 다른 코드를 넣어서 실행시점에 바꿔치기 할 수 있는 그런 실행코드를 짜는 것이 더 현명하다.
    }
}
