package com.zbib.hiresync.builder;

import com.zbib.hiresync.dto.ApplicationListResponse;
import com.zbib.hiresync.dto.ApplicationRequest;
import com.zbib.hiresync.dto.ApplicationResponse;
import com.zbib.hiresync.dto.JobApplicationListResponse;
import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.enums.ApplicationStatus;
import java.time.LocalDateTime;

public class ApplicationBuilder {

  public static ApplicationResponse buildApplicationResponse(Application application) {
    return ApplicationResponse.builder()
        .id(application.getId())
        .jobId(application.getJob().getId())
        .firstName(application.getFirstName())
        .lastName(application.getLastName())
        .email(application.getEmail())
        .phoneNumber(application.getPhoneNumber())
        .linkedInUrl(application.getLinkedinUrl())
        .websiteUrl(application.getWebsiteUrl())
        .cvUrl(application.getCvUrl())
        .status(application.getStatus())
        .referredBy(application.getReferredBy())
        .appliedAt(application.getCreatedAt())
        .build();
  }

  public static JobApplicationListResponse buildJobApplicationListResponse(
      Application application) {
    return JobApplicationListResponse.builder()
        .id(application.getId())
        .firstName(application.getFirstName())
        .lastName(application.getLastName())
        .email(application.getEmail())
        .status(application.getStatus())
        .createdAt(application.getCreatedAt())
        .build();
  }

  public static Application buildApplication(ApplicationRequest request, Job job) {
    return Application.builder()
        .job(job)
        .firstName(request.getFirstName())
        .lastName(request.getLastName())
        .email(request.getEmail())
        .phoneNumber(request.getPhoneNumber())
        .linkedinUrl(request.getLinkedInUrl())
        .websiteUrl(request.getWebsiteUrl())
        .cvUrl(request.getCvUrl())
        .status(ApplicationStatus.NEW)
        .referredBy(request.getReferredBy())
        .createdAt(LocalDateTime.now())
        .build();
  }

  public static ApplicationListResponse buildApplicationListResponse(Application application) {
    return ApplicationListResponse.builder()
        .id(application.getId())
        .firstName(application.getFirstName())
        .lastName(application.getLastName())
        .email(application.getEmail())
        .status(application.getStatus())
        .createdAt(application.getCreatedAt())
        .job(
            ApplicationListResponse.ApplicationJobResponse.builder()
                .id(application.getJob().getId())
                .title(application.getJob().getTitle())
                .department(application.getJob().getDepartment())
                .status(application.getJob().getStatus())
                .build())
        .build();
  }
}
