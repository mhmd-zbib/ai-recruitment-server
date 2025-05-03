package com.zbib.hiresync.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration to disable certain logging features during tests.
 */
@TestConfiguration
@Profile("test")
public class TestLoggingConfig {
    
    /**
     * Override the method logging enabled property for tests
     */
    @Bean
    @Primary
    public Boolean methodLoggingEnabled() {
        return false;
    }
}
