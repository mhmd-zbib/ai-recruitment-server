package com.zbib.hiresync.dto;

import com.zbib.hiresync.enums.ApplicationStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class JobApplicationListResponse {
  private UUID id;
  private String firstName;
  private String lastName;
  private String email;
  private ApplicationStatus status;
  private LocalDateTime createdAt;
}
