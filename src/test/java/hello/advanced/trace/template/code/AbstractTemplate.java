package hello.advanced.trace.template.code;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractTemplate {
    //변하지 않는 시간 측정 로직.
    public void execute(){
        long startTime = System.currentTimeMillis();
        //비지니스 로직 실행
        //핵심 비지니스로직을 상속으로 풀어낸다.
        call();
        //비지니스 로직 종료
        long endTime = System.currentTimeMillis();
        long resultTime = endTime - startTime;
        log.info("resultTime = {}",resultTime);
    }
    //변하지 않는 부분은 위에 적고 변하는 부분에 대해서는 밑에 call을 만들어서 상속관계와 오버라이딩으로 풀어낸다.
    protected abstract void call();
}
