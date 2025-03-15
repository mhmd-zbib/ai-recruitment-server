package com.zbib.hiresync.exceptions;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class JobException extends AppException {

    private JobException(HttpStatus status, String message, String details) {
        super(status,
                message,
                details);
    }

    public static JobException jobNotFound(UUID jobId) {
        return new JobException(HttpStatus.NOT_FOUND,
                "Job not found",
                "The job with ID " + jobId + " does not exist");
    }

    public static JobException invalidJobData(String details) {
        return new JobException(HttpStatus.BAD_REQUEST,
                "Invalid job data",
                details);
    }

    public static JobException unauthorizedAccess(UUID jobId) {
        return new JobException(HttpStatus.FORBIDDEN,
                "Unauthorized access",
                "You do not have permission to access or modify job with ID " + jobId);
    }

    public static JobException updateFailed(UUID jobId, String details) {
        return new JobException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to update job",
                "Could not update job with ID " + jobId + ". " + details);
    }
}