package com.zbib.hiresync.builder;

import com.zbib.hiresync.dto.JobListResponseDTO;
import com.zbib.hiresync.dto.JobRequestDTO;
import com.zbib.hiresync.dto.JobResponseDTO;
import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.enums.JobStatus;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for building Job-related objects
 */
public class JobBuilder {

    /**
     * Builds a Job entity from a JobRequestDTO and User
     *
     * @param jobRequestDTO the DTO containing job information
     * @param user the user creating the job
     * @return a new Job entity
     */
    public static Job buildJobEntity(JobRequestDTO jobRequestDTO, User user) {
        return Job.builder()
                .user(user)
                .title(jobRequestDTO.getTitle())
                .department(jobRequestDTO.getDepartment())
                .description(jobRequestDTO.getDescription())
                .responsibilities(jobRequestDTO.getResponsibilities())
                .qualifications(jobRequestDTO.getQualifications())
                .benefits(jobRequestDTO.getBenefits())
                .yearsOfExperience(jobRequestDTO.getYearsOfExperience())
                .locationType(jobRequestDTO.getLocationType())
                .employmentType(jobRequestDTO.getEmploymentType())
                .status(JobStatus.ACTIVE)
                .minSalary(jobRequestDTO.getMinSalary())
                .maxSalary(jobRequestDTO.getMaxSalary())
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Builds a JobResponseDTO from a Job entity
     *
     * @param job the job entity
     * @return a new JobResponseDTO
     */
    public static JobResponseDTO buildJobResponseDTO(Job job) {
        return JobResponseDTO.builder()
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

    /**
     * Builds a JobListResponseDTO from a Page of Job entities
     *
     * @param jobPage the page of job entities
     * @return a new JobListResponseDTO
     */
    public static JobListResponseDTO buildJobListResponseDTO(Page<Job> jobPage) {
        List<JobResponseDTO> content = jobPage.getContent().stream()
                .map(JobBuilder::buildJobResponseDTO)
                .collect(Collectors.toList());
        
        return JobListResponseDTO.builder()
                .content(content)
                .pageNo(jobPage.getNumber())
                .pageSize(jobPage.getSize())
                .totalElements(jobPage.getTotalElements())
                .totalPages(jobPage.getTotalPages())
                .last(jobPage.isLast())
                .build();
    }
}