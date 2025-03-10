package com.zbib.hiresync.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception class for job-related errors.
 * Provides factory methods for common job-related exceptions.
 */
public class JobException extends AppException {
    
    private JobException(HttpStatus status, String message, String details) {
        super(status, message, details);
    }
    
    private JobException(HttpStatus status, String message) {
        super(status, message);
    }
    
    /**
     * Creates an exception for when a job is not found.
     * 
     * @param jobId the ID of the job that was not found
     * @return a new JobException
     */
    public static JobException jobNotFound(String jobId) {
        return new JobException(
                HttpStatus.NOT_FOUND,
                "Job not found",
                "The job with ID " + jobId + " does not exist"
        );
    }
    
    /**
     * Creates an exception for when a job creation fails due to validation errors.
     * 
     * @param details specific details about the validation failure
     * @return a new JobException
     */
    public static JobException invalidJobData(String details) {
        return new JobException(
                HttpStatus.BAD_REQUEST,
                "Invalid job data",
                details
        );
    }
    
    /**
     * Creates an exception for when a user is not authorized to access or modify a job.
     * 
     * @param jobId the ID of the job
     * @return a new JobException
     */
    public static JobException unauthorizedAccess(String jobId) {
        return new JobException(
                HttpStatus.FORBIDDEN,
                "Unauthorized access",
                "You do not have permission to access or modify job with ID " + jobId
        );
    }
    
    /**
     * Creates an exception for when a job update operation fails.
     * 
     * @param jobId the ID of the job
     * @param details specific details about the update failure
     * @return a new JobException
     */
    public static JobException updateFailed(String jobId, String details) {
        return new JobException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to update job",
                "Could not update job with ID " + jobId + ". " + details
        );
    }
}