package hello.advanced.app.trace;

//로그의 상태정보
//로그를 종료할 때 사용되기도 한다.
public class TraceStatus {
    private TraceId traceId;
    private Long startTimeMs;
    //진행시간을 알기위한 스타트 타이머 시간.
    //로그 종료때 start에서 종료시간을 빼주면 진행시간을 구할 수 있다.
    private String message;
    //처음 로그가 떴을 때, 메세지가 마지막에도 필요함
    //처음 수행시 로그 내용이 마지막에도 필요.


    public TraceStatus(TraceId traceId, Long startTimeMs, String message) {
        this.traceId = traceId;
        this.startTimeMs = startTimeMs;
        this.message = message;
    }

    public TraceId getTraceId() {
        return traceId;
    }

    public Long getStartTimeMs() {
        return startTimeMs;
    }

    public String getMessage() {
        return message;
    }
}
