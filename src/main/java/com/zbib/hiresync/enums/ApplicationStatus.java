package com.zbib.hiresync.enums;

/**
 * Enum representing the status of a job application
 */
public enum ApplicationStatus {
    /**
     * Application has been submitted but not yet reviewed
     */
    SUBMITTED,
    
    /**
     * Application is under review by the employer/recruiter
     */
    UNDER_REVIEW,
    
    /**
     * Applicant has been shortlisted for further consideration
     */
    SHORTLISTED,
    
    /**
     * Applicant has been invited for an interview
     */
    INTERVIEW_SCHEDULED,
    
    /**
     * Interview has been completed
     */
    INTERVIEWED,
    
    /**
     * Applicant has received a job offer
     */
    OFFER_EXTENDED,
    
    /**
     * Applicant has accepted the job offer
     */
    OFFER_ACCEPTED,
    
    /**
     * Applicant has rejected the job offer
     */
    OFFER_REJECTED,
    
    /**
     * Application has been rejected by the employer/recruiter
     */
    REJECTED,
    
    /**
     * Application has been withdrawn by the applicant
     */
    WITHDRAWN
} 