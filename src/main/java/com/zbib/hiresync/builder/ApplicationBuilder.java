package com.zbib.hiresync.builder;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.zbib.hiresync.dto.ApplicationListResponse;
import com.zbib.hiresync.dto.ApplicationRequest;
import com.zbib.hiresync.dto.ApplicationResponse;
import com.zbib.hiresync.dto.JobApplicationListResponse;
import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.JobPosting;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.enums.ApplicationStatus;

@Component
public class ApplicationBuilder {

  public ApplicationResponse buildApplicationResponse(Application application) {
    return ApplicationResponse.builder()
        .id(application.getId())
        .jobId(application.getJob().getId())
        .firstName(application.getFirstName())
        .lastName(application.getLastName())
        .email(application.getEmail())
        .phoneNumber(application.getPhoneNumber())
        .websiteUrl(application.getWebsiteUrl())
        .linkedInUrl(application.getLinkedinUrl())
        .cvUrl(application.getCvUrl())
        .referredBy(application.getReferredBy())
        .status(application.getStatus())
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

  public Application buildApplication(ApplicationRequest request, User user, JobPosting job) {
    return Application.builder()
        .job(job)
        .user(user)
        .firstName(request.getFirstName())
        .lastName(request.getLastName())
        .email(request.getEmail())
        .phoneNumber(request.getPhoneNumber())
        .websiteUrl(request.getWebsiteUrl())
        .linkedinUrl(request.getLinkedInUrl())
        .cvUrl(request.getCvUrl())
        .referredBy(request.getReferredBy())
        .status(ApplicationStatus.NEW)
        .build();
  }

  public ApplicationListResponse buildApplicationListResponse(Application application) {
    return ApplicationListResponse.builder()
        .id(application.getId())
        .firstName(application.getFirstName())
        .lastName(application.getLastName())
        .email(application.getEmail())
        .status(application.getStatus())
        .createdAt(application.getCreatedAt())
        .job(ApplicationListResponse.ApplicationJobResponse.builder()
            .id(application.getJob().getId())
            .title(application.getJob().getTitle())
            .department(application.getJob().getDepartment())
            .status(application.getJob().getStatus())
            .build())
        .build();
  }
}
