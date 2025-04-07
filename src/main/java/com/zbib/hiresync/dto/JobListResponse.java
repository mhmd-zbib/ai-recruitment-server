package com.zbib.hiresync.dto;

import com.zbib.hiresync.enums.EmploymentType;
import com.zbib.hiresync.enums.JobStatus;
import com.zbib.hiresync.enums.LocationType;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobListResponse {
  private UUID id;
  private String title;
  private String department;
  private LocationType locationType;
  private EmploymentType employmentType;
  private JobStatus status;
  private int yearsOfExperience;
  private int minSalary;
  private int maxSalary;
  private LocalDateTime createdAt;
}
