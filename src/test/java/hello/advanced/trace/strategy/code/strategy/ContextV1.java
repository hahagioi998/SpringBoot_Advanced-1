package hello.advanced.trace.strategy.code.strategy;

import lombok.extern.slf4j.Slf4j;

/*
 * 컨텍스트를 만들어서 필드에 전력을 보관하는 방식
 * 컨텍스트는 대단히 큰 문맥을 만들어서 변하지 않는 컨텍스트를 만들고
 *
 * 이 컨텍스트 하위에 전략 인터페이스를 만들어서 그것을 구현하는 strategyLogic1, strategyLogic2를 구현합니다.
 *
 * 변하지 않는 로직을 가지고 있는 템플릿 역할을 하는 코드.
 * 전략패턴에서는 이것을 문맥이라고 지칭함.
 *
 * 그 문맥에 위임한 구현 클래스(strategy) 전략 이 바뀔때 마다 바뀐다고 생각하면 된다.
 *
 * 전략 패턴의 장점은 인터페이스만 의존하기 때문에 하위 개체들이 바뀌어도 큰 문맥의 컨텍스트에서는 의존성 결여 문제가
 * 전혀 생기지 않는다는 장점이 있다.
 *
 * 스프링에서 사용하는 의존관계주입 방식이 바로 이방식입니다.
 *
 * 결국엔 템플릿 메서드 패턴인 상속을 위한 구현 보다는 위임에 중점을 둔 전략패턴이 더 좋습니다.
 */
@Slf4j
public class ContextV1 {
    private Strategy strategy;

    public ContextV1(Strategy strategy){
        this.strategy = strategy;
    }

    public void execute(){
        long startTime = System.currentTimeMillis();
        //비지니스 로직 실행
        strategy.call(); // 위임
        //비지니스 로직 종료
        long endTime = System.currentTimeMillis();
        long resultTime = endTime - startTime;
        log.info("resultTime = {}",resultTime);
    }
}
