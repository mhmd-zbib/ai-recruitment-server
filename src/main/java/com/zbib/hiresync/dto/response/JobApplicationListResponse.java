package com.zbib.hiresync.dto.response;

import com.zbib.hiresync.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicationListResponse {
    private UUID id;
    private UUID jobId;
    private String jobTitle;
    private String companyName;
    private String firstName;
    private String lastName;
    private String applicantEmail;
    private String resumeUrl;
    private String linkedinUrl;
    private ApplicationStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}