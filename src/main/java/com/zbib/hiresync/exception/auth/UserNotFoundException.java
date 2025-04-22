package com.zbib.hiresync.exception.auth;

import com.zbib.hiresync.exception.BaseException;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends BaseException {
    
    public UserNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
    
    public UserNotFoundException() {
        super("User not found", HttpStatus.NOT_FOUND);
    }
} 