package com.zbib.hiresync.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

public class ApplicationException extends AppException {

    private ApplicationException(HttpStatus status, String userMessage, String logMessage) {
        super(status, userMessage, logMessage);
    }

    public static ApplicationException notFound(UUID applicationId) {
        return new ApplicationException(NOT_FOUND,
                "Application not found",
                formatLogMessage("Application not found with ID", applicationId.toString()));
    }
    
    public static ApplicationException alreadyApplied(UUID jobId, String email) {
        return new ApplicationException(CONFLICT,
                "You have already applied to this job",
                formatLogMessage("User " + email + " attempted to apply again to job", jobId.toString()));
    }
    
    public static ApplicationException jobNotActive(UUID jobId) {
        return new ApplicationException(BAD_REQUEST,
                "This job is no longer accepting applications",
                formatLogMessage("Application attempt for inactive job", jobId.toString()));
    }

    public static ApplicationException viewNotAuthorized(UUID applicationId, String username) {
        return new ApplicationException(FORBIDDEN,
                "You do not have permission to view this application",
                formatLogMessage("User " + username + " attempted to view application not owned by them, application ID", applicationId.toString()));
    }

    public static ApplicationException updateNotAuthorized(UUID applicationId, String username) {
        return new ApplicationException(FORBIDDEN,
                "You do not have permission to update this application",
                formatLogMessage("User " + username + " attempted to update application not owned by them, application ID", applicationId.toString()));
    }

    public static ApplicationException deleteNotAuthorized(UUID applicationId, String username) {
        return new ApplicationException(FORBIDDEN,
                "You do not have permission to delete this application",
                formatLogMessage("User " + username + " attempted to delete application not owned by them, application ID", applicationId.toString()));
    }

    private static String formatLogMessage(String message, String value) {
        return String.format("%s: [%s]", message, value);
    }
}