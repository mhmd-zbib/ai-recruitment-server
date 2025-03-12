package com.zbib.hiresync.dto;

import com.zbib.hiresync.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApplicationResponse {
    private UUID id;
    private UUID jobId;
    private String jobTitle;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String linkedInUrl;
    private String websiteUrl;
    private String cvUrl;
    private String coverLetter;
    private ApplicationStatus status;
    private String referredBy;
    private LocalDateTime appliedAt;
    private LocalDateTime lastUpdatedAt;
}