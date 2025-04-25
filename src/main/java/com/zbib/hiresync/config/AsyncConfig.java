package com.zbib.hiresync.config;

import com.zbib.hiresync.logging.ThreadContextTaskDecorator;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@RequiredArgsConstructor
public class AsyncConfig {

  private final ThreadContextTaskDecorator threadContextTaskDecorator;

  @Bean(name = "taskExecutor")
  public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("hiresync-async-");
    executor.setTaskDecorator(threadContextTaskDecorator);
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(60);
    executor.initialize();
    return executor;
  }

  @Bean(name = "emailExecutor")
  public Executor emailExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(50);
    executor.setThreadNamePrefix("email-async-");
    executor.setTaskDecorator(threadContextTaskDecorator);
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.initialize();
    return executor;
  }
}
