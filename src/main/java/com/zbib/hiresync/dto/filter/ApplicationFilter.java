package com.zbib.hiresync.dto.filter;

import com.zbib.hiresync.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Data Transfer Object for filtering job applications
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationFilter {
    
    private UUID jobPostId;
    private String applicantEmail;
    private String applicantName;
    private Set<ApplicationStatus> statuses;
    private LocalDateTime submittedAfter;
    private LocalDateTime submittedBefore;
    private LocalDateTime updatedAfter;
    private LocalDateTime updatedBefore;
    private Boolean hasResume;
    private Boolean hasCoverLetter;
} 