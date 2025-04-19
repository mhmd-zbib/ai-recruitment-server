package com.zbib.hiresync.config;

import com.zbib.hiresync.logging.ThreadContextTaskDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous task execution with ThreadContext propagation.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Creates a task executor that preserves ThreadContext across thread boundaries
     * using the ThreadContextTaskDecorator.
     */
    @Bean("taskExecutor")
    public Executor taskExecutor(ThreadContextTaskDecorator threadContextTaskDecorator) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("hiresync-async-");
        executor.setTaskDecorator(threadContextTaskDecorator);
        executor.initialize();
        return executor;
    }
} 