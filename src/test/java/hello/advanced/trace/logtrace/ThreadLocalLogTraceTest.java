package hello.advanced.trace.logtrace;

import hello.advanced.app.trace.TraceStatus;
import hello.advanced.app.trace.logtrace.FieldLogTrace;
import hello.advanced.app.trace.logtrace.ThreadLocalLogTrace;
import org.junit.jupiter.api.Test;

public class ThreadLocalLogTraceTest {

    ThreadLocalLogTrace trace = new ThreadLocalLogTrace();

    @Test
    public void begin_end_level2() {
        //given
        TraceStatus status1 = trace.begin("testCode Request1");
        TraceStatus status2 = trace.begin("testCode Request2");
        //when
        trace.end(status2);
        trace.end(status1);
        //then
        //14:34:18.523 [main] INFO hello.advanced.app.trace.logtrace.FieldLogTrace - [2c4c978b]  testCode Request1
        //14:34:18.527 [main] INFO hello.advanced.app.trace.logtrace.FieldLogTrace - [2c4c978b] |--> testCode Request2
        //14:34:18.528 [main] INFO hello.advanced.app.trace.logtrace.FieldLogTrace - [2c4c978b] |<-- testCode Request2 time=1ms
        //14:34:18.528 [main] INFO hello.advanced.app.trace.logtrace.FieldLogTrace - [2c4c978b]  testCode Request1 time=7ms
    }

    @Test
    public void begin_exception_level2() {
        //given
        TraceStatus status1 = trace.begin("testCode Request1");
        TraceStatus status2 = trace.begin("testCode Request2");
        //when
        trace.exception(status2,new IllegalStateException());
        trace.exception(status1,new IllegalStateException());
        //then
        //14:34:18.523 [main] INFO hello.advanced.app.trace.logtrace.FieldLogTrace - [2c4c978b]  testCode Request1
        //14:34:18.527 [main] INFO hello.advanced.app.trace.logtrace.FieldLogTrace - [2c4c978b] |--> testCode Request2
        //14:34:18.528 [main] INFO hello.advanced.app.trace.logtrace.FieldLogTrace - [2c4c978b] |<X- testCode Request2 time=1ms
        //14:34:18.528 [main] INFO hello.advanced.app.trace.logtrace.FieldLogTrace - [2c4c978b]  testCode Request1 time=7ms
    }
}
