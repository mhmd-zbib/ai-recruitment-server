package com.zbib.hiresync.builder;

import com.zbib.hiresync.dto.JobListResponse;
import com.zbib.hiresync.dto.JobRequest;
import com.zbib.hiresync.dto.JobResponse;
import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.enums.JobStatus;

import java.time.LocalDateTime;

public class JobBuilder {

    public static Job buildJobEntity(JobRequest jobRequest, User user) {
        return Job
                .builder()
                .user(user)
                .title(jobRequest.getTitle())
                .department(jobRequest.getDepartment())
                .description(jobRequest.getDescription())
                .responsibilities(jobRequest.getResponsibilities())
                .qualifications(jobRequest.getQualifications())
                .benefits(jobRequest.getBenefits())
                .yearsOfExperience(jobRequest.getYearsOfExperience())
                .locationType(jobRequest.getLocationType())
                .employmentType(jobRequest.getEmploymentType())
                .status(JobStatus.ACTIVE)
                .minSalary(jobRequest.getMinSalary())
                .maxSalary(jobRequest.getMaxSalary())
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static JobResponse buildJobResponseDTO(Job job) {
        return JobResponse
                .builder()
                .id(job.getId())
                .title(job.getTitle())
                .department(job.getDepartment())
                .description(job.getDescription())
                .responsibilities(job.getResponsibilities())
                .qualifications(job.getQualifications())
                .benefits(job.getBenefits())
                .yearsOfExperience(job.getYearsOfExperience())
                .locationType(job.getLocationType())
                .employmentType(job.getEmploymentType())
                .status(job.getStatus())
                .minSalary(job.getMinSalary())
                .maxSalary(job.getMaxSalary())
                .createdAt(job.getCreatedAt())
                .build();
    }

    public static JobListResponse buildJobListResponseDTO(Job job) {
        return JobListResponse
                .builder()
                .id(job.getId())
                .title(job.getTitle())
                .department(job.getDepartment())
                .locationType(job.getLocationType())
                .employmentType(job.getEmploymentType())
                .status(job.getStatus())
                .yearsOfExperience(job.getYearsOfExperience())
                .minSalary(job.getMinSalary())
                .maxSalary(job.getMaxSalary())
                .createdAt(job.getCreatedAt())
                .build();
    }
}