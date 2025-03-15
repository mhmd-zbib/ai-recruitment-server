package com.zbib.hiresync.builder;

import com.zbib.hiresync.dto.JobListResponse;
import com.zbib.hiresync.dto.JobRequest;
import com.zbib.hiresync.dto.JobResponse;
import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.enums.JobStatus;

import java.time.LocalDateTime;

/**
 * Utility class for building Job-related objects
 */
public class JobBuilder {

    /**
     * Builds a Job entity from a JobRequestDTO and User
     *
     * @param jobRequest the DTO containing job information
     * @param user the user creating the job
     * @return a new Job entity
     */
    public static Job buildJobEntity(JobRequest jobRequest, User user) {
        return Job.builder()
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
        return JobResponse.builder()
                .id(job.getId())
                .userId(job.getUser().getId())
                .username(job.getUser().getUsername())
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
        return JobListResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .department(job.getDepartment())
                .applications(10)
                .match(0.92)
                .createdAt(job.getCreatedAt())
                .build();
    }
}