package com.zbib.hiresync.dto.response;

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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobListResponse {
    private UUID id;
    private String title;
    private String companyName;
    private String location;
    private WorkplaceType workplaceType;
    private EmploymentType employmentType;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private String currency;
    private boolean active;
    private LocalDateTime visibleUntil;
    private Set<String> skills;
    private Set<String> tags;
    private long applicationCount;
    private LocalDateTime createdAt;
} 