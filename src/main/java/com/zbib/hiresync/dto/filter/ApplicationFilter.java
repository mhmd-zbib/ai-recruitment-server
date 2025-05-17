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
 * Filter for job applications with various criteria
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationFilter {
    private UUID jobId;
    private String searchQuery;
    private ApplicationStatus status;
    private Set<ApplicationStatus> statuses;
    private Set<String> skills;
    private Integer appliedWithinDays;
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;
    private LocalDateTime updatedAfter;
    private LocalDateTime updatedBefore;
    private String applicantEmail;
    private String phoneNumber;
}
