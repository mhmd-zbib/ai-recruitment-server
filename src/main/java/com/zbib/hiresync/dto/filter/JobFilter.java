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
    
    private String searchQuery;
    
    private String city;
    private String country;
    private Boolean remoteAllowed;
    
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private String currency;
    
    private Set<EmploymentType> employmentTypes;
    private Set<WorkplaceType> workplaceTypes;

    private Integer postedWithinDays;
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;

    private Boolean active;
    
    private UUID createdById;
} 