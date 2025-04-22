package com.zbib.hiresync.validator;

import com.zbib.hiresync.entity.JobPost;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.exception.security.UnauthorizedException;
import com.zbib.hiresync.exception.application.ApplicationAlreadyExistException;
import com.zbib.hiresync.exception.jobpost.JobPostNotFoundException;
import com.zbib.hiresync.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Validator for job application operations
 * Handles permissions and validation logic
 */
@Component
@RequiredArgsConstructor
public class ApplicationValidator {
    
    private final ApplicationRepository applicationRepository;
    
    public void validateApplicationCreation(JobPost jobPost, String applicantEmail) {
        validateJobPostActive(jobPost);
        validateApplicationNotExists(jobPost, applicantEmail);
    }
    
    public void validateJobPostApplicationAccess(JobPost jobPost, User currentUser) {
        if (jobPost.getCreatedBy() == null || !jobPost.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to access this job post's applications");
        }
    }
    
    public void validateApplicationListAccess(User currentUser) {
        // No validation needed here, as Spring Security ensures authentication
        // Any additional business rule validations can be added here
    }
    
    private void validateJobPostActive(JobPost jobPost) {
        if (!jobPost.isActive()) {
            throw new JobPostNotFoundException();
        }
    }
    
    private void validateApplicationNotExists(JobPost jobPost, String applicantEmail) {
        if (applicationRepository.existsByJobPostAndApplicantEmail(jobPost, applicantEmail)) {
            throw new ApplicationAlreadyExistException();
        }
    }
} 