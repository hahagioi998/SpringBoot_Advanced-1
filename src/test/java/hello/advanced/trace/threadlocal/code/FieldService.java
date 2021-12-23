package hello.advanced.trace.threadlocal.code;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FieldService {
    private String nameStore;

    public String logic(String name){
        log.info("저장 name={} -> nameStore={}",name,nameStore);
        nameStore = name;
        sleep(1000);
        log.info("조회 nameStore={}",nameStore);
         //저장하는데 1초가 걸린다고 가정.
        return nameStore;

        /*
         * 값 제거는 remove()
         * 스레드로컬에서 주의할 점은 remove메서드를 통해서 저장된 값을 꼭 제거해주어야 합니다.
         */
    }

    private void sleep(int millis) {
        try{
            Thread.sleep(millis);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }

}
