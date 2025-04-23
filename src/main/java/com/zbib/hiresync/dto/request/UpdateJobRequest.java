package com.zbib.hiresync.dto.request;

import com.zbib.hiresync.enums.EmploymentType;
import com.zbib.hiresync.enums.WorkplaceType;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO for updating an existing job
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateJobRequest {

    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    @Size(min = 50, max = 20000, message = "Description must be between 50 and 20,000 characters")
    private String description;

    @Size(min = 50, max = 10000, message = "Requirements must be between 50 and 10,000 characters")
    private String requirements;

    @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
    private String companyName;

    private String location;

    private WorkplaceType workplaceType;

    private EmploymentType employmentType;

    @Positive(message = "Minimum salary must be positive")
    private BigDecimal minSalary;

    @Positive(message = "Maximum salary must be positive")
    private BigDecimal maxSalary;

    @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
    private String currency;

    private Set<String> skills;

    private Set<String> tags;

    private LocalDateTime visibleUntil;
} 