package hello.advanced.trace.template;

import hello.advanced.trace.template.code.AbstractTemplate;
import hello.advanced.trace.template.code.SubClassLogic1;
import hello.advanced.trace.template.code.SubClassLogic2;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class TemplateMethodTest {
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
    /*
     14:53:36.397 [main] INFO hello.advanced.trace.template.TemplateMethodTest - 비지니스 로직1 실행
        14:53:36.399 [main] INFO hello.advanced.trace.template.TemplateMethodTest - resultTime = 3
        14:53:36.401 [main] INFO hello.advanced.trace.template.TemplateMethodTest - 비지니스 로직1 실행
        14:53:36.401 [main] INFO hello.advanced.trace.template.TemplateMethodTest - resultTime = 0

        사실상 변하는 구간은 핵심로직만 변화될 뿐,(비지니스로직) 변하지 않는 부분은 시간을 측정하는 부분을 제외한 나머지는  변하지 않는다.
        여기서 템플릿 메서드 패턴을 적용하게 되면 메서드로 따로 뽑아내기 어려웠던 변하지 않는 부분에 대해서 중복성을 줄여가며 실행할 수 있다.
     */

    @Test
    void templateMethodV1(){
        AbstractTemplate template1 = new SubClassLogic1();
        template1.execute();

        AbstractTemplate template2 = new SubClassLogic2();
        template2.execute();
    }

    //익명클래스로 정의하기
    @Test
    private void templateMethodV2(){
        AbstractTemplate template1 = new AbstractTemplate(){
            @Override
            protected void call() {
                log.info("비지니스 로직1 실행");
            }
        };
        template1.execute();

        AbstractTemplate template2 = new AbstractTemplate(){
            @Override
            protected void call() {
                log.info("비지니스 로직2 실행");
            }
        };
        template2.execute();
    }
}
