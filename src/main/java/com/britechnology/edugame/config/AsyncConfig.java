package com.britechnology.edugame.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-email-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setTaskDecorator(copyMdcTaskDecorator());
        executor.initialize();
        return executor;
    }

    private TaskDecorator copyMdcTaskDecorator() {
        return runnable -> {
            var contextMap = org.slf4j.MDC.getCopyOfContextMap();
            return () -> {
                var previous = org.slf4j.MDC.getCopyOfContextMap();
                try {
                    if (contextMap != null) {
                        org.slf4j.MDC.setContextMap(contextMap);
                    } else {
                        org.slf4j.MDC.clear();
                    }
                    runnable.run();
                } finally {
                    if (previous != null) {
                        org.slf4j.MDC.setContextMap(previous);
                    } else {
                        org.slf4j.MDC.clear();
                    }
                }
            };
        };
    }
}
