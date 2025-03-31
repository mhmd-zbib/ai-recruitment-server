package com.zbib.hiresync.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AppException extends RuntimeException {

  private final HttpStatus status;
  private final String message;
  private final String details;

  public AppException(HttpStatus status, String message, String details) {
    super(message);
    this.status = status;
    this.message = message;
    this.details = details;
  }

  public AppException(HttpStatus status, String message) {
    this(status, message, null);
  }
}
