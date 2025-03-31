package com.zbib.hiresync.dto;

import com.zbib.hiresync.enums.ApplicationStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicationFilter {
  private String query;
  private ApplicationStatus status;
  private LocalDateTime minCreatedAt;
  private LocalDateTime maxCreatedAt;
}
