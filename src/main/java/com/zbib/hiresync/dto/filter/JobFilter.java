package com.zbib.hiresync.dto.filter;

import com.zbib.hiresync.enums.EmploymentType;
import com.zbib.hiresync.enums.WorkplaceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Unified filter for job searches across both public feed and HR dashboard views
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobFilter {
    
    // Text search
    private String searchQuery;
    
    // Location filtering
    private String city;
    private String country;
    private Boolean remoteAllowed;
    
    // Salary filtering
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private String currency;
    
    // Type filtering
    private Set<EmploymentType> employmentTypes;
    private Set<WorkplaceType> workplaceTypes;
    
    // Skills and tags filtering
    private Set<String> skills;
    private Set<String> tags;
    
    // Date filtering
    private Integer postedWithinDays;
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;
    private LocalDateTime visibleAfter;
    private LocalDateTime visibleBefore;
    
    // Visibility filtering
    private Boolean active;
    private Boolean includeInactive;
    
    // HR specific filtering
    private UUID createdById;
    
    // Flag to determine if this is a feed request (public) or HR dashboard request
    private Boolean isFeedRequest;
} 