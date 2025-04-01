package com.zbib.hiresync.dto;

import com.zbib.hiresync.enums.EmploymentType;
import com.zbib.hiresync.enums.LocationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobRequest {

  @NotBlank(message = "Title is required")
  @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
  private String title;

  @NotBlank(message = "Department is required")
  @Size(max = 50, message = "Department name cannot exceed 50 characters")
  private String department;

  @NotBlank(message = "Description is required")
  @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
  private String description;

  @NotBlank(message = "Responsibilities are required")
  @Size(min = 10, max = 2000, message = "Responsibilities must be between 10 and 2000 characters")
  private String responsibilities;

  @NotBlank(message = "Qualifications are required")
  @Size(min = 10, max = 2000, message = "Qualifications must be between 10 and 2000 characters")
  private String qualifications;

  @Size(max = 1000, message = "Benefits cannot exceed 1000 characters")
  private String benefits;

  @Min(value = 0, message = "Years of experience cannot be negative")
  @Max(value = 30, message = "Years of experience cannot exceed 30")
  private int yearsOfExperience;

  @NotNull(message = "Location type is required")
  private LocationType locationType;

  @NotNull(message = "Employment type is required")
  private EmploymentType employmentType;

  @Min(value = 0, message = "Minimum salary cannot be negative")
  private int minSalary;

  @Min(value = 0, message = "Maximum salary cannot be negative")
  private int maxSalary;
}
