package com.zbib.hiresync.service;

import com.zbib.hiresync.builder.JobBuilder;
import com.zbib.hiresync.dto.JobFilter;
import com.zbib.hiresync.dto.JobListResponseDTO;
import com.zbib.hiresync.dto.JobRequestDTO;
import com.zbib.hiresync.dto.JobResponseDTO;
import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.enums.EmploymentType;
import com.zbib.hiresync.enums.JobStatus;
import com.zbib.hiresync.enums.LocationType;
import com.zbib.hiresync.repository.JobRepository;
import com.zbib.hiresync.repository.UserRepository;
import com.zbib.hiresync.specification.JobSpecification;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    public JobResponseDTO createJob(JobRequestDTO jobRequestDTO, Long userId) {
        validateJobRequest(jobRequestDTO);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        Job job = JobBuilder.buildJobEntity(jobRequestDTO,
                user);
        Job savedJob = jobRepository.save(job);

        return JobBuilder.buildJobResponseDTO(savedJob);
    }


    public JobResponseDTO getJobById(UUID id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + id));

        return JobBuilder.buildJobResponseDTO(job);
    }


    public Page<JobListResponseDTO> getAllJobsWithFilters(JobFilter filter, Pageable pageable) {
        Page<Job> jobs = jobRepository.findAll(JobSpecification.filterJobs(filter), pageable);
        return JobBuilder.buildJobListResponseDTOPage(jobs);
    }

    private void validateJobRequest(JobRequestDTO jobRequestDTO) {
        if (jobRequestDTO.getMinSalary() > jobRequestDTO.getMaxSalary()) {
            throw new IllegalArgumentException("Minimum salary cannot be greater than maximum salary");
        }
    }
}