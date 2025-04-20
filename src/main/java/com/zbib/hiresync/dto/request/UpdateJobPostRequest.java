package com.zbib.hiresync.dto.request;

import com.zbib.hiresync.enums.EmploymentType;
import com.zbib.hiresync.enums.WorkplaceType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * DTO for updating an existing job post
 * All fields are optional to allow partial updates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateJobPostRequest {

    @Size(min = 5, max = 100, message = "Title must be between 5 and 100 characters")
    private String title;

    @Size(max = 5000, message = "Description must be less than 5000 characters")
    private String description;

    @Size(max = 500, message = "Requirements must be less than 500 characters")
    private String requirements;

    @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
    private String companyName;

    @Size(max = 255, message = "Location must be less than 255 characters")
    private String location;

    private WorkplaceType workplaceType;

    private EmploymentType employmentType;

    private BigDecimal minSalary;
    
    private BigDecimal maxSalary;
    
    @Size(max = 3, message = "Currency code must be 3 characters")
    private String currency;

    private Boolean active;
    
    @Future(message = "Visibility end date must be in the future")
    private LocalDateTime visibleUntil;

    @Builder.Default
    private Set<String> skills = new HashSet<>();

    @Builder.Default
    private Set<String> tags = new HashSet<>();
} 