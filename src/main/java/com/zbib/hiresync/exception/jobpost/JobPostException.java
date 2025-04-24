package com.zbib.hiresync.exception.jobpost;

import com.zbib.hiresync.exception.BaseException;
import org.springframework.http.HttpStatus;

public class JobPostException extends BaseException {
    public JobPostException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

    public JobPostException(String message, Throwable cause) {
        super(message, HttpStatus.BAD_REQUEST, cause);
    }
} 