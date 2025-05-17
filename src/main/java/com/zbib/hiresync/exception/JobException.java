package com.zbib.hiresync.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

public class JobException extends AppException {

    private JobException(HttpStatus status, String userMessage, String logMessage) {
        super(status, userMessage, logMessage);
    }

    public static JobException notFound(UUID jobId) {
        return new JobException(NOT_FOUND,
                "Job not found",
                formatLogMessage("Job not found with ID", jobId.toString()));
    }

    public static JobException notActive(UUID jobId) {
        return new JobException(BAD_REQUEST,
                "This job listing is no longer active",
                formatLogMessage("Attempted to access inactive job with ID", jobId.toString()));
    }

    public static JobException hasApplications(UUID jobId) {
        return new JobException(CONFLICT,
                "Cannot delete job with existing applications",
                formatLogMessage("Attempted to delete job with existing applications, job ID", jobId.toString()));
    }

    public static JobException updateNotAuthorized(UUID jobId, String username) {
        return new JobException(FORBIDDEN,
                "You do not have permission to update this job",
                formatLogMessage("User " + username + " attempted to update job not owned by them, job ID", jobId.toString()));
    }

    public static JobException deleteNotAuthorized(UUID jobId, String username) {
        return new JobException(FORBIDDEN,
                "You do not have permission to delete this job",
                formatLogMessage("User " + username + " attempted to delete job not owned by them, job ID", jobId.toString()));
    }

    private static String formatLogMessage(String message, String value) {
        return String.format("%s: [%s]", message, value);
    }
}