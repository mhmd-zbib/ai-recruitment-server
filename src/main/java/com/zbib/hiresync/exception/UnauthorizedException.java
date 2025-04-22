package com.zbib.hiresync.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BaseException {

    public UnauthorizedException() {
        super("You are not authorized to access this resource", HttpStatus.FORBIDDEN);
    }
}