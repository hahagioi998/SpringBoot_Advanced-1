package hello.advanced.trace.threadlocal;

import hello.advanced.trace.threadlocal.code.FieldService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class FieldServiceTest {

    private FieldService fieldService = new FieldService();

    @Test
    public void field() {
        //given
        log.info("main start");
        Runnable userA = ()-> {
            fieldService.logic("userA");
        };

        Runnable userB = ()-> {
            fieldService.logic("userB");
        };

        Thread threadA = new Thread(userA);
        threadA.setName("thread-A");
        Thread threadB = new Thread(userB);
        threadB.setName("thread-B");
        //when

        threadA.start();
        sleep(2000); //2초만큼 쉴건데, A가 완전히 끝나고 B를 실행 (동시성 문제가 없는 경우)
                           // 우리가 FIELDSERVICE 클래스에서 이름 저장하는 로직을 얼마나 걸려서 실행했는가? (1초) 2초는 충분하다.
                            //만약 A가 지금 스레드를 처리하는 시간이 1초인데 메인스레드가 만약 0.1초만 쉬고 다른 스레드인 B를 START 시킨다면?
                            //A의 1000밀리세컨드 시간이 끝나지도 않은 채 B가 스레드를 시작하여 공유자원인 NAMESTORE를 건드려서 USERB가 저장되어있을것이다.
        threadB.start();
        sleep(3000);
        //then
        /*
        * 트래픽이 한번에 많이 몰리면 확률상 발생할 확률이 더커짐.
        * STATIC이나 싱글통의 필드의 동시성 문제를 해결한다.
        *
        * 이게 읽기만하면 상관은 없는데 어디선가 자꾸 값을 바꾸기 시작하면 그때부터 동시성 문제가 생긴다.
        *
        * 쓰레드 로컬.이란?
        *
        * 그 해당 스레드만 구분되어서 데이터를 제공한다.
        * 예를 들어 창고에서 A와 B가 동시에 쓰는 것 말고 따로 구분해서 보관해준다.
        *
        *
        * */
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
