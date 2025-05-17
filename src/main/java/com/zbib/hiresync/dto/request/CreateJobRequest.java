package com.zbib.hiresync.dto.request;

import com.zbib.hiresync.enums.EmploymentType;
import com.zbib.hiresync.enums.WorkplaceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateJobRequest {

    @NotBlank(message = "Job title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    @NotBlank(message = "Job description is required")
    @Size(min = 50, max = 20000, message = "Description must be between 50 and 20,000 characters")
    private String description;

    @NotBlank(message = "Job requirements are required")
    @Size(min = 50, max = 10000, message = "Requirements must be between 50 and 10,000 characters")
    private String requirements;

    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
    private String companyName;

    private String location;

    @NotNull(message = "Workplace type is required")
    private WorkplaceType workplaceType;

    @NotNull(message = "Employment type is required")
    private EmploymentType employmentType;

    @Positive(message = "Minimum salary must be positive")
    private BigDecimal minSalary;

    @Positive(message = "Maximum salary must be positive")
    private BigDecimal maxSalary;

    @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
    private String currency;

    private Set<String> skills;

    private Set<String> tags;

    @NotNull(message = "Visible until date is required")
    private LocalDateTime visibleUntil;

    private boolean active;
}