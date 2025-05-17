package com.zbib.hiresync.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

public class UserException extends AppException {

    private UserException(HttpStatus status, String userMessage, String logMessage) {
        super(status, userMessage, logMessage);
    }

    public static UserException notFound(UUID userId) {
        return new UserException(NOT_FOUND,
                "User not found",
                formatLogMessage("User not found with ID", userId.toString()));
    }

    public static UserException notFound(String email) {
        return new UserException(NOT_FOUND,
                "User not found",
                formatLogMessage("User not found with email", email));
    }

    public static UserException accountLocked(String email) {
        return new UserException(FORBIDDEN,
                "Account is locked. Please contact support.",
                formatLogMessage("Attempted to access locked account with email", email));
    }

    public static UserException unauthorizedAccess(String email, String resource) {
        return new UserException(FORBIDDEN,
                "You do not have permission to access this resource",
                formatLogMessage("User " + email + " attempted unauthorized access to", resource));
    }

    private static String formatLogMessage(String message, String value) {
        return String.format("%s: [%s]", message, value);
    }
}