package com.zbib.hiresync.dto.response;

import com.zbib.hiresync.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response DTO containing statistics about a job
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobStatsResponse {
    private long totalViews;
    private long totalApplications;
    private Map<ApplicationStatus, Long> applicationsByStatus;
    private Map<String, Long> applicantsByTopSkills;
} 