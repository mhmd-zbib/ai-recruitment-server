package com.zbib.hiresync.logging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingConfig {

    @Bean
    public SensitiveDataMasker sensitiveDataMasker() {
        return new SensitiveDataMasker();
    }
    
    @Bean
    public PerformanceMonitor performanceMonitor() {
        return new PerformanceMonitor();
    }
}