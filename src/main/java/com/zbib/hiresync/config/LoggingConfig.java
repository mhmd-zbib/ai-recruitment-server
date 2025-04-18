package com.zbib.hiresync.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.util.List;

/**
 * Configuration for the application's logging system.
 * Configures AOP for method logging and ensures all required components are available.
 */
@Configuration
@EnableAspectJAutoProxy
public class LoggingConfig {
    private static final Profiles DEV_PROFILES = Profiles.of("dev", "local");
    
    @Value("${hiresync.logging.sensitive-fields:password,token,secret,key,credential,ssn}")
    private List<String> sensitiveFields;

    private final Environment environment;

    public LoggingConfig(Environment environment) {
        this.environment = environment;
    }
    
    /**
     * Configures the Jackson ObjectMapper for JSON formatting in logs.
     * This ensures consistent JSON formatting across the application.
     * 
     * @return The configured ObjectMapper instance
     */
    @Bean
    public ObjectMapper loggingObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Configure mapper for logging - pretty print in dev, compact in prod
        if (environment.acceptsProfiles(DEV_PROFILES)) {
            mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
        }
        
        // Disable failing on unknown properties - robust logging
        mapper.disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        
        // Ensure dates are serialized as strings
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        return mapper;
    }
} 