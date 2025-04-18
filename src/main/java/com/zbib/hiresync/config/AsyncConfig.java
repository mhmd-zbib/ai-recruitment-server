package com.zbib.hiresync.config;

import com.zbib.hiresync.logging.MdcContextFilter.MdcTaskDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous task execution with MDC context propagation.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Creates a task executor that preserves MDC context across thread boundaries
     * using the MdcTaskDecorator.
     */
    @Bean("taskExecutor")
    public Executor taskExecutor(MdcTaskDecorator mdcTaskDecorator) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("hiresync-async-");
        executor.setTaskDecorator(mdcTaskDecorator);
        executor.initialize();
        return executor;
    }
} 