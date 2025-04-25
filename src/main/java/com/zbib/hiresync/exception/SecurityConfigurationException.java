package com.zbib.hiresync.exception;

import org.springframework.http.HttpStatus;

public class SecurityConfigurationException extends RuntimeException {
  private static final long serialVersionUID = 1L;
  private final HttpStatus status;

  public SecurityConfigurationException(String message, Throwable cause) {
    super(message, cause);
    this.status = HttpStatus.INTERNAL_SERVER_ERROR;
  }

  public SecurityConfigurationException(String message) {
    super(message);
    this.status = HttpStatus.INTERNAL_SERVER_ERROR;
  }

  public HttpStatus getStatus() {
    return status;
  }
}
