package com.zbib.hiresync.validator;

import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.exception.job.InvalidJobStateException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class JobValidator {

    /**
     * Validates that a job has all business rules satisfied
     */
    public void validateJobCompleteness(Job job) {
        validateSalaryRange(job);
    }
    
    /**
     * Validates the salary range of a job
     * Business rule: If one salary is provided, both must be provided
     * Business rule: Minimum salary cannot be greater than maximum salary
     */
    public void validateSalaryRange(Job job) {
        BigDecimal minSalary = job.getMinSalary();
        BigDecimal maxSalary = job.getMaxSalary();
        
        if (minSalary == null && maxSalary == null) {
            return;
        }
        
        if (minSalary == null || maxSalary == null) {
            throw InvalidJobStateException.invalidSalaryRange();
        }
        
        if (minSalary.compareTo(maxSalary) > 0) {
            throw InvalidJobStateException.minGreaterThanMax();
        }
    }
    
    /**
     * Validates that a job can be activated
     * Business rule: Job must have a future visibility date
     * Business rule: Job must satisfy all completeness requirements
     */
    public void validateCanBeActivated(Job job) {
        if (job.getVisibleUntil() == null || job.getVisibleUntil().isBefore(LocalDateTime.now())) {
            throw InvalidJobStateException.missingVisibleUntil();
        }
        
        try {
            validateJobCompleteness(job);
        } catch (InvalidJobStateException e) {
            throw InvalidJobStateException.cannotActivateIncomplete(e.getMessage());
        }
    }
} 