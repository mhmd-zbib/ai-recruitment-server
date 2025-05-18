package com.zbib.hiresync.dto.event;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ApplicationCreatedEvent {
    private final UUID applicationId;
    private final String jobPost;
    private final String resumeUrl;

    public ApplicationCreatedEvent(UUID applicationId, String jobPost, String resumeUrl) {
        this.applicationId = applicationId;
        this.jobPost = jobPost;
        this.resumeUrl = resumeUrl;
    }
}

