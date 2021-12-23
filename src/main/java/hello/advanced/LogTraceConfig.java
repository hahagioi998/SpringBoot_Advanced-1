package hello.advanced;

import hello.advanced.app.trace.logtrace.FieldLogTrace;
import hello.advanced.app.trace.logtrace.LogTrace;
import hello.advanced.app.trace.logtrace.ThreadLocalLogTrace;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LogTraceConfig {

//    @Bean
//    public LogTrace logTrace(){
//        return new FieldLogTrace();
//    }

    @Bean
    public LogTrace logTrace(){
        return new ThreadLocalLogTrace();
    }
    /*
     * 스레드 풀 문제 remove문제
     */
}
