package com.zbib.hiresync.builder;

import com.zbib.hiresync.dto.ApplicationCreateRequest;
import com.zbib.hiresync.dto.ApplicationListResponse;
import com.zbib.hiresync.dto.ApplicationResponse;
import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.enums.ApplicationStatus;

import java.time.LocalDateTime;

public class ApplicationBuilder {

    public static ApplicationResponse buildApplicationResponse(Application application) {
        return ApplicationResponse.builder()
                .id(application.getId())
                .jobId(application.getJob()
                        .getId())
                .firstName(application.getFirstName())
                .lastName(application.getLastName())
                .email(application.getEmail())
                .phoneNumber(application.getPhoneNumber())
                .linkedInUrl(application.getLinkedInUrl())
                .websiteUrl(application.getWebsiteUrl())
                .cvUrl(application.getCvUrl())
                .status(application.getStatus())
                .referredBy(application.getReferredBy())
                .appliedAt(application.getCreatedAt())
                .build();
    }

    public static ApplicationListResponse buildApplicationListResponse(Application application) {
        return ApplicationListResponse.builder()
                .id(application.getId())
                .firstName(application.getFirstName())
                .lastName(application.getLastName())
                .email(application.getEmail())
                .jobTitle(application.getJob()
                        .getTitle())
                .jobId(application.getJob()
                        .getId())
                .status(application.getStatus())
                .createdAt(application.getCreatedAt())
                .build();
    }

    public static Application buildApplication(ApplicationCreateRequest request, Job job) {
        return Application.builder()
                .job(job)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .linkedInUrl(request.getLinkedInUrl())
                .websiteUrl(request.getWebsiteUrl())
                .cvUrl(request.getCvUrl())
                .status(ApplicationStatus.PENDING)
                .referredBy(request.getReferredBy())
                .createdAt(LocalDateTime.now())
                .build();
    }
}