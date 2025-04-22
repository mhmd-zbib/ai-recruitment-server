package com.zbib.hiresync.exception.application;

import org.springframework.http.HttpStatus;

public class ApplicationAlreadyExistException extends ApplicationException {
    public ApplicationAlreadyExistException() {
        super("You have already applied to this job", HttpStatus.CONFLICT);
    }
}