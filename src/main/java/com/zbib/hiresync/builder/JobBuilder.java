package com.zbib.hiresync.builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.zbib.hiresync.dto.JobListResponse;
import com.zbib.hiresync.dto.JobRequest;
import com.zbib.hiresync.dto.JobResponse;
import com.zbib.hiresync.entity.JobPosting;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.enums.JobStatus;

@Component
public class JobBuilder {

  public JobPosting buildJob(JobRequest request) {
    return JobPosting.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .requirements(request.getRequirements())
            .location(request.getLocation())
            .department(request.getDepartment())
            .employmentType(request.getEmploymentType())
            .status(request.getStatus())
            .build();
  }

  public JobResponse buildJobResponse(JobPosting job) {
    return JobResponse.builder()
            .id(job.getId())
            .title(job.getTitle())
            .description(job.getDescription())
            .requirements(job.getRequirements())
            .location(job.getLocation())
            .department(job.getDepartment())
            .employmentType(job.getEmploymentType())
            .status(job.getStatus())
            .createdAt(job.getCreatedAt())
            .updatedAt(job.getUpdatedAt())
            .build();
  }

  public List<JobResponse> buildJobListResponse(List<JobPosting> jobs) {
    return jobs.stream().map(this::buildJobResponse).collect(Collectors.toList());
  }

  // Static methods needed by JobService
  public static JobPosting buildJobEntity(JobRequest request, User user) {
    return JobPosting.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .requirements(request.getRequirements())
            .location(request.getLocation())
            .department(request.getDepartment())
            .employmentType(request.getEmploymentType())
            .status(request.getStatus())
            .build();
  }

  public static JobResponse buildJobResponseDTO(JobPosting job) {
    return JobResponse.builder()
            .id(job.getId())
            .title(job.getTitle())
            .description(job.getDescription())
            .requirements(job.getRequirements())
            .location(job.getLocation())
            .department(job.getDepartment())
            .employmentType(job.getEmploymentType())
            .status(job.getStatus())
            .createdAt(job.getCreatedAt())
            .updatedAt(job.getUpdatedAt())
            .build();
  }

  public static JobListResponse buildJobListResponseDTO(JobPosting job) {
    JobListResponse response = new JobListResponse();
    response.setId(job.getId());
    response.setTitle(job.getTitle());
    response.setDepartment(job.getDepartment());
    response.setLocationType(job.getLocationType());
    response.setEmploymentType(job.getEmploymentType());
    response.setStatus(job.getStatus());
    response.setMinSalary(job.getMinSalary());
    response.setMaxSalary(job.getMaxSalary());
    response.setCreatedAt(job.getCreatedAt());
    return response;
  }
}
