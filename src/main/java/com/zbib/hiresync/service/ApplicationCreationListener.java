package com.zbib.hiresync.service;

import com.zbib.hiresync.dto.event.ApplicationCreatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

@Component
@EnableAsync
public class ApplicationCreationListener {

    private final ApplicationService processingService;

    public ApplicationCreationListener(ApplicationService processingService) {
        this.processingService = processingService;
    }

    @EventListener
    @Async
    public void onApplicationCreated(ApplicationCreatedEvent event) {
        processingService.process(event);
    }
}
