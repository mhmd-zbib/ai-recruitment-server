package com.zbib.hiresync.builder;

import com.zbib.hiresync.dto.JobListResponse;
import com.zbib.hiresync.dto.JobRequest;
import com.zbib.hiresync.dto.JobResponse;
import com.zbib.hiresync.entity.JobPosting;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.enums.JobStatus;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import lombok.NonNull;

@Component
public class JobBuilder {

  public JobPosting buildJob(@NonNull JobRequest request) {
    validateJobRequest(request);
    return JobPosting.builder()
        .title(request.getTitle())
        .description(request.getDescription())
        .responsibilities(request.getResponsibilities())
        .qualifications(request.getQualifications())
        .benefits(request.getBenefits())
        .yearsOfExperience(request.getYearsOfExperience())
        .locationType(request.getLocationType())
        .department(request.getDepartment())
        .employmentType(request.getEmploymentType())
        .minSalary(request.getMinSalary())
        .maxSalary(request.getMaxSalary())
        .status(JobStatus.OPEN)
        .build();
  }

  public JobPosting buildJobWithUser(@NonNull JobRequest request, @NonNull User user) {
    validateJobRequest(request);
    return JobPosting.builder()
        .user(user)
        .title(request.getTitle())
        .description(request.getDescription())
        .responsibilities(request.getResponsibilities())
        .qualifications(request.getQualifications())
        .benefits(request.getBenefits())
        .yearsOfExperience(request.getYearsOfExperience())
        .locationType(request.getLocationType())
        .department(request.getDepartment())
        .employmentType(request.getEmploymentType())
        .minSalary(request.getMinSalary())
        .maxSalary(request.getMaxSalary())
        .status(JobStatus.OPEN)
        .build();
  }

  public JobResponse buildJobResponse(@NonNull JobPosting job) {
    return JobResponse.builder()
        .id(job.getId())
        .title(job.getTitle())
        .description(job.getDescription())
        .responsibilities(job.getResponsibilities())
        .qualifications(job.getQualifications())
        .benefits(job.getBenefits())
        .yearsOfExperience(job.getYearsOfExperience())
        .locationType(job.getLocationType())
        .department(job.getDepartment())
        .employmentType(job.getEmploymentType())
        .minSalary(job.getMinSalary())
        .maxSalary(job.getMaxSalary())
        .status(job.getStatus())
        .createdAt(job.getCreatedAt())
        .updatedAt(job.getUpdatedAt())
        .build();
  }

  public List<JobResponse> buildJobListResponse(@NonNull List<JobPosting> jobs) {
    return jobs.stream()
        .filter(Objects::nonNull)
        .map(this::buildJobResponse)
        .collect(Collectors.toList());
  }

  public JobListResponse buildJobListSummary(@NonNull JobPosting job) {
    return JobListResponse.builder()
        .id(job.getId())
        .title(job.getTitle())
        .department(job.getDepartment())
        .locationType(job.getLocationType())
        .employmentType(job.getEmploymentType())
        .status(job.getStatus())
        .minSalary(job.getMinSalary())
        .maxSalary(job.getMaxSalary())
        .createdAt(job.getCreatedAt())
        .build();
  }

  private void validateJobRequest(JobRequest request) {
    Objects.requireNonNull(request.getTitle(), "Job title cannot be null");
    Objects.requireNonNull(request.getDescription(), "Job description cannot be null");
    Objects.requireNonNull(request.getDepartment(), "Department cannot be null");
    Objects.requireNonNull(request.getEmploymentType(), "Employment type cannot be null");
    
    if (request.getMinSalary() > 0 && request.getMaxSalary() > 0 
        && request.getMinSalary() > request.getMaxSalary()) {
      throw new IllegalArgumentException("Minimum salary cannot be greater than maximum salary");
    }
  }
}
