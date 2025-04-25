package com.zbib.hiresync.exception;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;

public class AuthenticationConfigurationException extends AuthenticationException {
  private static final long serialVersionUID = 1L;
  private final HttpStatus status;

  public AuthenticationConfigurationException(String message, Throwable cause) {
    super(message, cause);
    this.status = HttpStatus.INTERNAL_SERVER_ERROR;
  }

  public AuthenticationConfigurationException(String message) {
    super(message);
    this.status = HttpStatus.INTERNAL_SERVER_ERROR;
  }

  public HttpStatus getStatus() {
    return status;
  }
}
