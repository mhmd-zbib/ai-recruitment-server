package com.zbib.hiresync.validation;

import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class JobValidator {

    public void validateJobCompleteness(Job job) {
        Map<String, String> errors = new HashMap<>();
        
        if (job.getTitle() == null || job.getTitle().isBlank()) {
            errors.put("title", "Job title is required");
        }
        
        if (job.getDescription() == null || job.getDescription().isBlank()) {
            errors.put("description", "Job description is required");
        }
        
        if (job.getRequirements() == null || job.getRequirements().isBlank()) {
            errors.put("requirements", "Job requirements are required");
        }
        
        if (job.getCompanyName() == null || job.getCompanyName().isBlank()) {
            errors.put("companyName", "Company name is required");
        }
        
        if (job.getWorkplaceType() == null) {
            errors.put("workplaceType", "Workplace type is required");
        }
        
        if (job.getEmploymentType() == null) {
            errors.put("employmentType", "Employment type is required");
        }
        
        if (job.getVisibleUntil() == null) {
            errors.put("visibleUntil", "Visibility end date is required");
        }
        
        if (job.getMinSalary() != null || job.getMaxSalary() != null) {
            if (job.getMinSalary() != null && job.getMinSalary().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                errors.put("minSalary", "Minimum salary must be positive");
            }
            
            if (job.getMaxSalary() != null && job.getMaxSalary().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                errors.put("maxSalary", "Maximum salary must be positive");
            }
            
            if (job.getMinSalary() != null && job.getMaxSalary() != null 
                    && job.getMinSalary().compareTo(job.getMaxSalary()) > 0) {
                errors.put("salary", "Minimum salary cannot be greater than maximum salary");
            }
            
            if ((job.getMinSalary() != null || job.getMaxSalary() != null) 
                    && (job.getCurrency() == null || job.getCurrency().isBlank())) {
                errors.put("currency", "Currency is required when salary is provided");
            }
        }
        
        if (!errors.isEmpty()) {
            throw ValidationException.missingRequiredFields(errors);
        }
    }
}