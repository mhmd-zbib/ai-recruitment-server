package com.zbib.hiresync.dto.filter;

import com.zbib.hiresync.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object for filtering job applications
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationFilter {
    
    private String search;
    private UUID jobPostId;
    private String applicantEmail;
    private ApplicationStatus status;
    private List<ApplicationStatus> statuses;
    private List<String> skills;
    private LocalDateTime submittedAfter;
    private LocalDateTime submittedBefore;
} 