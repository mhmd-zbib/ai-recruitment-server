package com.zbib.hiresync.dto;

import com.zbib.hiresync.enums.ApplicationStatus;
import com.zbib.hiresync.enums.EmploymentType;
import com.zbib.hiresync.enums.JobStatus;
import com.zbib.hiresync.enums.LocationType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for filtering job applications based on various criteria. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationFilter {

  // General search query
  private String query;

  // Job-related filters
  private List<Long> jobId;
  private List<LocationType> locationType;
  private List<EmploymentType> employmentType;
  private List<JobStatus> jobStatus;

  // Experience and salary ranges
  private Integer minExperience;
  private Integer maxExperience;
  private Integer minSalary;
  private Integer maxSalary;

  // Application status (multiple values)
  private List<ApplicationStatus> status;

  // Date filters
  private LocalDateTime minJobCreatedAt;
  private LocalDateTime maxJobCreatedAt;
  private LocalDateTime minCreatedAt;
  private LocalDateTime maxCreatedAt;
}
