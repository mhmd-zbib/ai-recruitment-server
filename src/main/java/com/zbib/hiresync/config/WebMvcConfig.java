package com.zbib.hiresync.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration to ensure proper resource handling,
 * particularly for Swagger UI with context path
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Add resource handlers for Swagger UI
        registry.addResourceHandler(contextPath + "/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/4.15.5/");
        
        registry.addResourceHandler(contextPath + "/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
        
        // Regular Swagger paths without context path for internal resolution
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/4.15.5/");
        
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Redirect to Swagger UI
        registry.addRedirectViewController(contextPath + "/swagger-ui/", 
                contextPath + "/swagger-ui/index.html");
        
        registry.addRedirectViewController("/swagger-ui/", 
                "/swagger-ui/index.html");
    }
}