package com.zbib.hiresync.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

public class AuthException extends AppException {

    private AuthException(HttpStatus status, String userMessage, String logMessage) {
        super(status, userMessage, logMessage);
    }

    public static AuthException accessDenied(String message, String username) {
        return new AuthException(FORBIDDEN,
                "Access denied: " + message,
                "User " + username + " attempted unauthorized access: " + message);
    }

    public static AuthException accessDenied(String resourceType, UUID resourceId, String username) {
        return new AuthException(FORBIDDEN,
                "You do not have permission to access this " + resourceType,
                "User " + username + " attempted to access " + resourceType + " with ID " + resourceId + " without permission");
    }

    public static AuthException emailAlreadyExists(String email) {
        return new AuthException(CONFLICT,
                "Email already exists",
                formatLogMessage("Attempted to register with an email that already exists", email));
    }

    public static AuthException invalidCredentials() {
        return new AuthException(UNAUTHORIZED,
                "Invalid credentials provided",
                "Authentication attempt with invalid credentials");
    }

    public static AuthException invalidToken(String token) {
        return new AuthException(UNAUTHORIZED,
                "Invalid or expired token",
                formatLogMessage("Token validation failed for token", token));
    }

    public static AuthException tokenExpired() {
        return new AuthException(UNAUTHORIZED,
                "Your session has expired. Please log in again",
                "Attempt to access with expired session token");
    }

    public static AuthException userNotFound(String identifier) {
        return new AuthException(HttpStatus.NOT_FOUND,
                "User not found",
                formatLogMessage("User not found for identifier", identifier));
    }

    private static String formatLogMessage(String message, String value) {
        return String.format("%s: [%s]", message, value);
    }
}
