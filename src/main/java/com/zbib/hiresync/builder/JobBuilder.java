package com.zbib.hiresync.builder;

import com.zbib.hiresync.dto.JobListResponse;
import com.zbib.hiresync.dto.JobRequest;
import com.zbib.hiresync.dto.JobResponse;
import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.enums.JobStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class JobBuilder {

  public Job buildJob(JobRequest request) {
    Job job = new Job();
    job.setTitle(request.getTitle());
    job.setDescription(request.getDescription());
    job.setDepartment(request.getDepartment());
    job.setResponsibilities(request.getResponsibilities());
    job.setQualifications(request.getQualifications());
    job.setBenefits(request.getBenefits());
    job.setYearsOfExperience(request.getYearsOfExperience());
    job.setLocationType(request.getLocationType());
    job.setEmploymentType(request.getEmploymentType());
    job.setStatus(JobStatus.ACTIVE);
    job.setMinSalary(request.getMinSalary());
    job.setMaxSalary(request.getMaxSalary());
    job.setCreatedAt(LocalDateTime.now());
    return job;
  }

  public JobResponse buildJobResponse(Job job) {
    JobResponse response = new JobResponse();
    response.setId(job.getId());
    response.setTitle(job.getTitle());
    response.setDescription(job.getDescription());
    response.setDepartment(job.getDepartment());
    response.setResponsibilities(job.getResponsibilities());
    response.setQualifications(job.getQualifications());
    response.setBenefits(job.getBenefits());
    response.setYearsOfExperience(job.getYearsOfExperience());
    response.setLocationType(job.getLocationType());
    response.setEmploymentType(job.getEmploymentType());
    response.setStatus(job.getStatus());
    response.setMinSalary(job.getMinSalary());
    response.setMaxSalary(job.getMaxSalary());
    response.setCreatedAt(job.getCreatedAt());
    return response;
  }

  public List<JobResponse> buildJobListResponse(List<Job> jobs) {
    return jobs.stream().map(this::buildJobResponse).collect(Collectors.toList());
  }

  // Static methods needed by JobService
  public static JobResponse buildJobResponseDTO(Job job) {
    JobResponse response = new JobResponse();
    response.setId(job.getId());
    response.setTitle(job.getTitle());
    response.setDescription(job.getDescription());
    response.setDepartment(job.getDepartment());
    response.setResponsibilities(job.getResponsibilities());
    response.setQualifications(job.getQualifications());
    response.setBenefits(job.getBenefits());
    response.setYearsOfExperience(job.getYearsOfExperience());
    response.setLocationType(job.getLocationType());
    response.setEmploymentType(job.getEmploymentType());
    response.setStatus(job.getStatus());
    response.setMinSalary(job.getMinSalary());
    response.setMaxSalary(job.getMaxSalary());
    response.setCreatedAt(job.getCreatedAt());
    return response;
  }

  public static Job buildJobEntity(JobRequest request, User user) {
    Job job = new Job();
    job.setTitle(request.getTitle());
    job.setDescription(request.getDescription());
    job.setDepartment(request.getDepartment());
    job.setResponsibilities(request.getResponsibilities());
    job.setQualifications(request.getQualifications());
    job.setBenefits(request.getBenefits());
    job.setYearsOfExperience(request.getYearsOfExperience());
    job.setLocationType(request.getLocationType());
    job.setEmploymentType(request.getEmploymentType());
    job.setStatus(JobStatus.ACTIVE);
    job.setMinSalary(request.getMinSalary());
    job.setMaxSalary(request.getMaxSalary());
    job.setCreatedAt(LocalDateTime.now());
    job.setUser(user);
    return job;
  }

  public static JobListResponse buildJobListResponseDTO(Job job) {
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
