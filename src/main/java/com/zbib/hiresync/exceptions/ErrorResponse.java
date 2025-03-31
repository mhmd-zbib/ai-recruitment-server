package com.zbib.hiresync.exceptions;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
  private int status;
  private String error;
  private String message;
  private String details;
  private String path;
  private LocalDateTime timestamp;
  private String requestId;
}
