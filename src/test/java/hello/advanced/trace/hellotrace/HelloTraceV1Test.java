package hello.advanced.trace.hellotrace;

import hello.advanced.app.trace.hellotrace.HelloTraceV1;
import hello.advanced.app.trace.TraceStatus;
import org.junit.jupiter.api.Test;

public class HelloTraceV1Test {
    @Test
    public void begin_end (){

        HelloTraceV1 trace = new HelloTraceV1();
        TraceStatus status = trace.begin("hello");
        trace.end(status);
//        15:14:34.509 [main] INFO hello.advanced.app.hellotrace.HelloTraceV1 - [77ef03cc]  hello
//        15:14:34.512 [main] INFO hello.advanced.app.hellotrace.HelloTraceV1 - [77ef03cc]  hello time=4ms
    }

    @Test
    public void begin_exception (){
        HelloTraceV1 trace = new HelloTraceV1();
        TraceStatus status = trace.begin("hello");
        trace.exception(status, new IllegalStateException());
        /*
        15:15:52.937 [main] INFO hello.advanced.app.hellotrace.HelloTraceV1 - [593073b1]  hello
        15:15:52.941 [main] INFO hello.advanced.app.hellotrace.HelloTraceV1 - [593073b1]  hello ex=java.lang.IllegalStateException
         */
    }
    
}
