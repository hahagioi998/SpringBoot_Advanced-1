package hello.advanced.trace.hellotrace;

import hello.advanced.app.trace.TraceStatus;
import hello.advanced.app.trace.hellotrace.HelloTraceV1;
import hello.advanced.app.trace.hellotrace.HelloTraceV2;
import org.junit.jupiter.api.Test;

public class HelloTraceV2Test {
    @Test
    public void begin_end (){

        HelloTraceV2 trace = new HelloTraceV2();
        TraceStatus status1 = trace.begin("hello");
        TraceStatus status2 = trace.beginSync(status1.getTraceId(), "hello");
        trace.end(status2);
        trace.end(status1);
        //15:53:03.842 [main] INFO hello.advanced.app.trace.hellotrace.HelloTraceV2 - [268ee808]  hello
        //15:53:03.848 [main] INFO hello.advanced.app.trace.hellotrace.HelloTraceV2 - [268ee808] |--> hello
        //15:53:03.848 [main] INFO hello.advanced.app.trace.hellotrace.HelloTraceV2 - [268ee808]  hello time=8ms
        //15:53:03.848 [main] INFO hello.advanced.app.trace.hellotrace.HelloTraceV2 - [268ee808] |<-- hello time=3ms
    }

    @Test
    public void begin_exception (){
        HelloTraceV2 trace = new HelloTraceV2();
        TraceStatus status1 = trace.begin("hello1");
        TraceStatus status2 = trace.beginSync(status1.getTraceId(), "hello2");
        trace.exception(status2,new IllegalStateException());
        trace.exception(status1,new IllegalStateException());
        //16:07:31.248 [main] INFO hello.advanced.app.trace.hellotrace.HelloTraceV2 - [1f01f0fa]  hello
        //16:07:31.253 [main] INFO hello.advanced.app.trace.hellotrace.HelloTraceV2 - [1f01f0fa] |--> hello
        //16:07:31.253 [main] INFO hello.advanced.app.trace.hellotrace.HelloTraceV2 - [1f01f0fa] |<-- hello time=2ms
        //16:07:31.253 [main] INFO hello.advanced.app.trace.hellotrace.HelloTraceV2 - [1f01f0fa]  hello time=7ms
    }
    
}
