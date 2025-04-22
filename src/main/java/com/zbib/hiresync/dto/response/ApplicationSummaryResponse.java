package com.zbib.hiresync.dto.response;

import com.zbib.hiresync.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for returning summarized job application details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationSummaryResponse {
    private UUID id;
    private UUID jobPostId;
    private String jobTitle;
    private String companyName;
    private String applicantName;
    private String applicantEmail;
    private ApplicationStatus status;
    private LocalDateTime createdAt;
} 