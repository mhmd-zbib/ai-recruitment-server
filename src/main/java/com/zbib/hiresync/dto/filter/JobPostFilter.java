package com.zbib.hiresync.dto.filter;

import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.enums.EmploymentType;
import com.zbib.hiresync.enums.WorkplaceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Data Transfer Object for filtering job posts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobPostFilter {
    
    private String search;
    private String city;
    private String country;
    private WorkplaceType workplaceType;
    private EmploymentType employmentType;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private String currency;
    private List<String> skills;
    private List<String> tags;
    private Boolean active;
    private Boolean includeInactive;
    private User createdBy;
} 