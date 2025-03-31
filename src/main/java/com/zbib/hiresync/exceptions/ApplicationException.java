package com.zbib.hiresync.exceptions;

import java.util.UUID;
import org.springframework.http.HttpStatus;

public class ApplicationException extends AppException {

  public ApplicationException(HttpStatus status, String message, String details) {
    super(status, message, details);
  }

  public static ApplicationException applicationNotFound(UUID id) {
    return new ApplicationException(
        HttpStatus.NOT_FOUND,
        "Application not found",
        "Application with id " + id + " does not exist");
  }
}
