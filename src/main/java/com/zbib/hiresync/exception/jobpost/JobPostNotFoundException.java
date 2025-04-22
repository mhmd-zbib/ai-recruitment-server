package com.zbib.hiresync.exception.jobpost;

import org.springframework.http.HttpStatus;
import java.util.UUID;

public class JobPostNotFoundException extends JobPostException {
    public JobPostNotFoundException() {
        super("Job post not found", HttpStatus.NOT_FOUND);
    }
}