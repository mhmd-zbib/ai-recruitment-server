package com.zbib.hiresync.dto.response;

import com.zbib.hiresync.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for returning job application details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponse {
    private UUID id;
    private UUID jobId;
    private String jobTitle;
    private String companyName;
    private String applicantName;
    private String applicantEmail;
    private String phoneNumber;
    private String coverLetter;
    private String resumeUrl;
    private String portfolioUrl;
    private String linkedinUrl;
    private ApplicationStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 