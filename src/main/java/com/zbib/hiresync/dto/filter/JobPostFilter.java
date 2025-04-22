package com.zbib.hiresync.dto.filter;

import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.enums.EmploymentType;
import com.zbib.hiresync.enums.WorkplaceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Filter for job posts with various criteria
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobPostFilter {
    
    private String search;
    private String location;
    private WorkplaceType workplaceType;
    private EmploymentType employmentType;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private Boolean active;
    private List<String> skills;
    private List<String> tags;
    private UUID createdById;
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;
    private LocalDateTime visibleAfter;
    private LocalDateTime visibleBefore;
    private String city;
    private String country;
    private String currency;
    private Boolean includeInactive;
    private User createdBy;
} 