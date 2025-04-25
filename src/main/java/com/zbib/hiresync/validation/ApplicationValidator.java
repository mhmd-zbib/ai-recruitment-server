package com.zbib.hiresync.validation;

import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.enums.ApplicationStatus;
import com.zbib.hiresync.exception.application.InvalidApplicationStateException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class ApplicationValidator {
    
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    
    private static final Pattern PHONE_PATTERN = 
        Pattern.compile("^\\+?[0-9]{10,15}$");
    
    private static final int MIN_COVER_LETTER_LENGTH = 100;
    private static final int MAX_COVER_LETTER_LENGTH = 5000;
    
    /**
     * Validates a new application for correctness before creation
     */
    public void validateNewApplication(Application application) {
        // Required fields validation
        if (application == null) {
            throw new InvalidApplicationStateException("Application cannot be null");
        }
        
        if (application.getJob() == null) {
            throw new InvalidApplicationStateException("Job cannot be null");
        }
        
        if (application.getApplicantName() == null || application.getApplicantName().trim().isEmpty()) {
            throw new InvalidApplicationStateException("Applicant name cannot be empty");
        }
        
        if (application.getApplicantEmail() == null || application.getApplicantEmail().trim().isEmpty()) {
            throw new InvalidApplicationStateException("Applicant email cannot be empty");
        }
        
        // Format validation
        if (application.getApplicantEmail() != null && 
            !EMAIL_PATTERN.matcher(application.getApplicantEmail()).matches()) {
            throw new InvalidApplicationStateException("Email format is invalid");
        }
        
        if (application.getPhoneNumber() != null && 
            !application.getPhoneNumber().isEmpty() && 
            !PHONE_PATTERN.matcher(application.getPhoneNumber()).matches()) {
            throw new InvalidApplicationStateException("Phone number format is invalid");
        }
        
        // Business logic validation
        if (!isValidJobForApplication(application.getJob())) {
            throw new InvalidApplicationStateException("Job is not active or has expired");
        }
        
        if (application.getCoverLetter() != null && 
            !application.getCoverLetter().isEmpty() && 
            application.getCoverLetter().length() < MIN_COVER_LETTER_LENGTH) {
            throw new InvalidApplicationStateException(
                "Cover letter is too short (minimum " + MIN_COVER_LETTER_LENGTH + " characters)");
        }
        
        if (application.getCoverLetter() != null && 
            !application.getCoverLetter().isEmpty() && 
            application.getCoverLetter().length() > MAX_COVER_LETTER_LENGTH) {
            throw new InvalidApplicationStateException(
                "Cover letter is too long (maximum " + MAX_COVER_LETTER_LENGTH + " characters)");
        }
    }
    
    /**
     * Validates a status transition for an application
     */
    public void validateStatusTransition(Application application, ApplicationStatus newStatus) {
        if (application == null) {
            throw new InvalidApplicationStateException("Application cannot be null");
        }
        
        if (newStatus == null) {
            throw new InvalidApplicationStateException("New status cannot be null");
        }
        
        if (application.isInTerminalState()) {
            throw new InvalidApplicationStateException(
                "Cannot change status from terminal state " + application.getStatus());
        }
        
        if (!isValidStatusTransition(application.getStatus(), newStatus)) {
            throw new InvalidApplicationStateException(
                "Invalid status transition from " + application.getStatus() + " to " + newStatus);
        }
        
        if (newStatus == ApplicationStatus.INTERVIEW_SCHEDULED && !hasContactInformation(application)) {
            throw new InvalidApplicationStateException("Cannot schedule interview without contact information");
        }
    }
    
    private boolean isValidJobForApplication(Job job) {
        return job != null && job.isActive() && !job.isExpired();
    }
    
    private boolean hasContactInformation(Application application) {
        return (application.getPhoneNumber() != null && !application.getPhoneNumber().trim().isEmpty())
            || (application.getApplicantEmail() != null && !application.getApplicantEmail().trim().isEmpty());
    }
    
    private boolean isValidStatusTransition(ApplicationStatus currentStatus, ApplicationStatus newStatus) {
        if (currentStatus == newStatus) {
            return true;
        }
        
        switch (currentStatus) {
            case SUBMITTED:
                // From SUBMITTED can go to any status except OFFER_ACCEPTED
                return newStatus != ApplicationStatus.OFFER_ACCEPTED;
                
            case UNDER_REVIEW:
                // From UNDER_REVIEW can go to any status except SUBMITTED
                return newStatus != ApplicationStatus.SUBMITTED;
                
            case INTERVIEW_SCHEDULED:
                // From INTERVIEW_SCHEDULED can go to INTERVIEWED, REJECTED, or WITHDRAWN
                return newStatus == ApplicationStatus.INTERVIEWED 
                    || newStatus == ApplicationStatus.REJECTED
                    || newStatus == ApplicationStatus.WITHDRAWN;
                
            case INTERVIEWED:
                // From INTERVIEWED can go to OFFER_EXTENDED, REJECTED, or WITHDRAWN
                return newStatus == ApplicationStatus.OFFER_EXTENDED 
                    || newStatus == ApplicationStatus.REJECTED
                    || newStatus == ApplicationStatus.WITHDRAWN;
                
            case OFFER_EXTENDED:
                // From OFFER_EXTENDED can go to OFFER_ACCEPTED, OFFER_REJECTED, or WITHDRAWN
                return newStatus == ApplicationStatus.OFFER_ACCEPTED 
                    || newStatus == ApplicationStatus.OFFER_REJECTED
                    || newStatus == ApplicationStatus.WITHDRAWN;
                
            case OFFER_ACCEPTED:
            case OFFER_REJECTED:
            case REJECTED:
            case WITHDRAWN:
                // Terminal states - no transitions allowed
                return false;
                
            case SHORTLISTED:
                // From SHORTLISTED can go to INTERVIEW_SCHEDULED, REJECTED, or WITHDRAWN
                return newStatus == ApplicationStatus.INTERVIEW_SCHEDULED
                    || newStatus == ApplicationStatus.REJECTED
                    || newStatus == ApplicationStatus.WITHDRAWN;
                
            default:
                return false;
        }
    }
} 