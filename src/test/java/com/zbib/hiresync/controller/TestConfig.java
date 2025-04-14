package com.zbib.hiresync.controller;

import com.zbib.hiresync.service.AuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static org.mockito.Mockito.mock;

@Configuration
public class TestConfig {
    
    @Bean
    public AuthService authService() {
        return mock(AuthService.class);
    }
} 