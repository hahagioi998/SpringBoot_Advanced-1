package hello.advanced.app.trace.logtrace;

import hello.advanced.app.trace.TraceId;
import hello.advanced.app.trace.TraceStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ThreadLocalLogTrace implements LogTrace {


    private static final String START_PREFIX = "-->";
    private static final String COMPLETE_PREFIX = "<--";
    private static final String EX_PREFIX = "<X-";

    //private TraceId traceIdHolder; //기존의 beginSync에서 단마다 traceId를 넘겨받아서 수동으로 id값은 유지하되 레벨을 올려주는 방식을
                                    //채택했지만 여기서는 필드를 하나 생성해서 여기에 traceId를 저장하기로 했다.
                                    //초기값은 null 이며 처음시작할 땐 null
    private ThreadLocal<TraceId> traceIdHolder = new ThreadLocal<>();
    private void syncTraceId(){
        TraceId traceId = traceIdHolder.get();
        if(traceId == null){
            traceIdHolder.set(new TraceId());              //처음일 경우에는 traceIdHolder가 비어있으니까 그냥 생으로 하나 만들어줍니다.
        }else{
            traceIdHolder.set(traceId.createNextId());//처음이 아닌 경우에는 이전 helloTrace 클래스에서 사용했던 id는 유지하되 level을 올려서
                                                         //리턴을 받는 메서드를 사용해줍니다.
                                                         //그러면 begin에서 syncTraceId를 사용해줌으로써 traceId는 값이 항상 보장된다.
            //        syncTraceId();
            //        TraceId traceId = traceIdHolder;
        }
    }

    @Override
    public TraceStatus begin(String message) {
        //TraceId traceId = new TraceId();
        syncTraceId();
        TraceId traceId = traceIdHolder.get();
        Long startTimeMs = System.currentTimeMillis();
        log.info("[{}] {} {}", traceId.getId(), addSpace(START_PREFIX, traceId.getLevel()), message);
        return new TraceStatus(traceId, startTimeMs, message);
    }

    @Override
    public void end(TraceStatus status) {
        complete(status,null);
    }

    @Override
    public void exception(TraceStatus status, Exception e) {
        complete(status,e);
    }

    private void complete(TraceStatus status, Exception e) {
        Long stopTime = System.currentTimeMillis();
        long resultTimeMs = stopTime - status.getStartTimeMs();
        TraceId traceId = status.getTraceId();

        if(e == null){
            log.info("[{}] {} {} time={}ms",status.getTraceId().getId(),addSpace(COMPLETE_PREFIX,traceId.getLevel()), status.getMessage(), resultTimeMs);
        }else{
            log.info("[{}] {} {} ex={}",status.getTraceId().getId(),addSpace(EX_PREFIX,traceId.getLevel()), status.getMessage(), e.toString());
        }

        releaseTraceId();
    }

    //public boolean isFirstLevel(){
    //    return level == 0;
    //}
    private void releaseTraceId() {
        TraceId traceId = traceIdHolder.get();
        if(traceId.isFirstLevel()){
            traceIdHolder.remove();//destroy
        }else{
            traceIdHolder.set(traceId.createPreviouslyId());
        }
        /*
         * complete가 끝난 후에는 이전단계의 레벨로 내려오면서 레벨이 0일때는 이제 trace 정보가 필요없게 되니까
         * 그리고 레벨이 0이 아니고 complete는 더 내려갈 구간이 있다는 것이니까 아이디는 유지하되 이전 값의 레벨을 만들어내는 메서드를 실행
         */
    }

    private static String addSpace(String prefix, int level){
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i<level; i++){
            sb.append((i==level-1)?"|"+prefix:"|    ");
        }
        return sb.toString();
    }
}
