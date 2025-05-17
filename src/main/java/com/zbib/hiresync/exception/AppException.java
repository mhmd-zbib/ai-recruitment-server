package com.zbib.hiresync.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AppException extends RuntimeException {
    private final HttpStatus status;
    private final String logMessage;

    protected AppException(HttpStatus status, String userMessage, String logMessage) {
        super(userMessage);
        this.status = status;
        this.logMessage = logMessage;
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }
}
