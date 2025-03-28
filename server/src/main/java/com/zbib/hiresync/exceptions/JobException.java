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
}