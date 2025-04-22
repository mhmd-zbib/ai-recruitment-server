package com.zbib.hiresync.dto.response;

import com.zbib.hiresync.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Response DTO containing statistics about applications for an HR user
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationStatsResponse {
    
    private long totalApplications;
    private Map<ApplicationStatus, Long> applicationsByStatus;
    private Map<UUID, Long> applicationsByJobPost;
} 