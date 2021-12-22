package hello.advanced.app.trace.hellotrace;

import hello.advanced.app.trace.TraceId;
import hello.advanced.app.trace.TraceStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HelloTraceV2 {

    private static final String START_PREFIX = "-->";
    private static final String COMPLETE_PREFIX = "<--";
    private static final String EX_PREFIX = "<X-";

    public TraceStatus begin(String message){
        TraceId traceId = new TraceId(); //begin이 들어오면 traceId를 새로 만들어낸다.
        Long startTimeMs = System.currentTimeMillis();
        log.info("[{}] {} {}", traceId.getId(), addSpace(START_PREFIX, traceId.getLevel()), message);
        //로그 출력
        return new TraceStatus(traceId, startTimeMs, message);
    } //시작할 때

//==================================추가==================================
    public TraceStatus beginSync(TraceId beforeTraceId,String message){
        TraceId traceId = beforeTraceId.createNextId();//이전 트레이스 아이디를 받아오게 함.
        //트레이스 아이디를 유지하되 레벨은 증가시킬 수 있음.

        Long startTimeMs = System.currentTimeMillis();
        log.info("[{}] {} {}", traceId.getId(), addSpace(START_PREFIX, traceId.getLevel()), message);
        //로그 출력
        return new TraceStatus(traceId, startTimeMs, message);
    } //시작할 때
//==================================추가==================================



    public void end(TraceStatus status){
        complete(status,null);
    } //로그가 종료될 때 status 상태정보를 받아옵니다.
    public void exception(TraceStatus status,Exception e){

        complete(status,e);
        // 예외가 터질 때 x도 표현해야 하며 예외 내용도 발생시켜야함
        //그러니까 begin 에서 end로 갈지 exception으로 갈지 결정하게 됩니다.
    } //시작때는 begin을 공용으로 사용하긴 하는데


    //로그 마지막에 발생시키는 메서드
    private void complete(TraceStatus status, Exception e) {
        Long stopTime = System.currentTimeMillis();
        long resultTimeMs = stopTime - status.getStartTimeMs();
        //status 에서 시작시간을 뽑아와서 현재 stop쳐준 시간에서 빼주면 그동안 걸렸던 시간이 나오게 된다.

        TraceId traceId = status.getTraceId();

        if(e == null){
            log.info("[{}] {} {} time={}ms",status.getTraceId().getId(),addSpace(COMPLETE_PREFIX,traceId.getLevel()), status.getMessage(), resultTimeMs);
        }else{
            log.info("[{}] {} {} ex={}",status.getTraceId().getId(),addSpace(EX_PREFIX,traceId.getLevel()), status.getMessage(), e.toString());
        }
    }

    private static String addSpace(String prefix, int level){
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i<level; i++){
            sb.append((i==level-1)?"|"+prefix:"|    ");
        }
        return sb.toString();
    }


}
