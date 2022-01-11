package hello.advanced.app.v5;

import hello.advanced.app.trace.callback.TraceCallback;
import hello.advanced.app.trace.callback.TraceTemplate;
import hello.advanced.app.trace.logtrace.LogTrace;
import hello.advanced.app.trace.template.AbstractTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceV5 {

    private final OrderRepositoryV5 orderRepositoryV5;
    private final TraceTemplate traceTemplate;

    public OrderServiceV5(OrderRepositoryV5 orderRepositoryV5, LogTrace logTrace) {
        this.orderRepositoryV5 = orderRepositoryV5;
        this.traceTemplate = new TraceTemplate(logTrace);
    }

    public void orderItem(String itemId){
        traceTemplate.execute("OrderService Log", new TraceCallback<Void>() {
            @Override
            public Void call() {
                orderRepositoryV5.save(itemId);
                return null;
            }
        });

    }

}
