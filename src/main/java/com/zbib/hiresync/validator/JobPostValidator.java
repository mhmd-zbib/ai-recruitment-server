package com.zbib.hiresync.validator;

import com.zbib.hiresync.entity.JobPost;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.exception.ResourceNotFoundException;
import com.zbib.hiresync.exception.security.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Validator for job post operations
 * Handles permissions and validation logic
 */
@Component
@RequiredArgsConstructor
public class JobPostValidator {
    
    public void validateOwnership(JobPost jobPost, User currentUser) {
        if (!isOwner(jobPost, currentUser)) {
            throw new UnauthorizedException("You are not authorized to modify this job post");
        }
    }
    
    public void validateJobPostAccess(JobPost jobPost, User currentUser) {
        if (!jobPost.isActive() && !isOwner(jobPost, currentUser)) {
            throw new ResourceNotFoundException("Job post not found with ID: " + jobPost.getId());
        }
    }
    
    private boolean isOwner(JobPost jobPost, User currentUser) {
        return jobPost.getCreatedBy() != null && 
               jobPost.getCreatedBy().getId().equals(currentUser.getId());
    }
} 