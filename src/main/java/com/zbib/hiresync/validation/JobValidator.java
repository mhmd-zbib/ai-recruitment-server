package com.zbib.hiresync.validation;

import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.exception.job.InvalidJobStateException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class JobValidator {

    public void validateJobCompleteness(Job job) {
        validateSalaryRange(job);
    }
    
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