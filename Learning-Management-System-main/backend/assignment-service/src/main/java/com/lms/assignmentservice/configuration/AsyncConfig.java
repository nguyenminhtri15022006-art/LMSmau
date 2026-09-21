package com.lms.assignmentservice.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    @Value("${assignment.async.judge.core-pool-size:4}")
    int judgeCorePoolSize;

    @Value("${assignment.async.judge.max-pool-size:8}")
    int judgeMaxPoolSize;

    @Value("${assignment.async.judge.queue-capacity:100}")
    int judgeQueueCapacity;

    @Value("${assignment.async.judge.thread-name-prefix:judge-}")
    String judgeThreadNamePrefix;

    @Bean(name = "judgeExecutor")
    public Executor judgeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(judgeCorePoolSize);
        executor.setMaxPoolSize(judgeMaxPoolSize);
        executor.setQueueCapacity(judgeQueueCapacity);
        executor.setThreadNamePrefix(judgeThreadNamePrefix);
        executor.initialize();
        return executor;
    }
}
