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
    
    public ValidationResult validateNewApplication(Application application) {
        return ValidationResult.startValidation()
            // Required fields validation
            .validateNotNull(application, "Application")
            .validateNotNull(application.getJob(), "Job")
            .validateNotEmpty(application.getApplicantName(), "Applicant name")
            .validateNotEmpty(application.getApplicantEmail(), "Applicant email")
            
            // Format validation
            .validateThat(
                application.getApplicantEmail() == null || 
                EMAIL_PATTERN.matcher(application.getApplicantEmail()).matches(),
                "Email format is invalid"
            )
            .validateThat(
                application.getPhoneNumber() == null || 
                application.getPhoneNumber().isEmpty() || 
                PHONE_PATTERN.matcher(application.getPhoneNumber()).matches(),
                "Phone number format is invalid"
            )
            
            // Business logic validation
            .validateThat(isValidJobForApplication(application.getJob()), 
                "Job is not active or has expired")
            .validateThat(
                application.getCoverLetter() == null || 
                application.getCoverLetter().isEmpty() || 
                application.getCoverLetter().length() >= MIN_COVER_LETTER_LENGTH,
                "Cover letter is too short (minimum " + MIN_COVER_LETTER_LENGTH + " characters)"
            )
            .validateThat(
                application.getCoverLetter() == null || 
                application.getCoverLetter().isEmpty() || 
                application.getCoverLetter().length() <= MAX_COVER_LETTER_LENGTH,
                "Cover letter is too long (maximum " + MAX_COVER_LETTER_LENGTH + " characters)"
            )
            .build();
    }
    
    public ValidationResult validateStatusTransition(Application application, ApplicationStatus newStatus) {
        return ValidationResult.startValidation()
            .validateNotNull(application, "Application")
            .validateNotNull(newStatus, "New status")
            .validateThat(!application.isInTerminalState(), 
                "Cannot change status from terminal state " + application.getStatus())
            .validateThat(isValidStatusTransition(application.getStatus(), newStatus),
                "Invalid status transition from " + application.getStatus() + " to " + newStatus)
            .validateThat(
                newStatus != ApplicationStatus.INTERVIEW_SCHEDULED || 
                hasContactInformation(application),
                "Cannot schedule interview without contact information"
            )
            .build();
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
    
    public void validateNewApplicationOrThrow(Application application) {
        ValidationResult result = validateNewApplication(application);
        if (!result.isValid()) {
            throw new InvalidApplicationStateException(
                String.join(", ", result.getErrors())
            );
        }
    }
    
    public void validateStatusTransitionOrThrow(Application application, ApplicationStatus newStatus) {
        ValidationResult result = validateStatusTransition(application, newStatus);
        if (!result.isValid()) {
            throw new InvalidApplicationStateException(
                String.join(", ", result.getErrors())
            );
        }
    }
} 