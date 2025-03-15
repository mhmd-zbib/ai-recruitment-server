package com.zbib.hiresync.service;

import com.zbib.hiresync.builder.JobBuilder;
import com.zbib.hiresync.dto.JobFilter;
import com.zbib.hiresync.dto.JobListResponse;
import com.zbib.hiresync.dto.JobRequest;
import com.zbib.hiresync.dto.JobResponse;
import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.exceptions.JobException;
import com.zbib.hiresync.repository.JobRepository;
import com.zbib.hiresync.repository.UserRepository;
import com.zbib.hiresync.specification.JobSpecification;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.zbib.hiresync.builder.JobBuilder.buildJobResponseDTO;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    public JobResponse createJob(JobRequest jobRequest, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        Job job = JobBuilder.buildJobEntity(jobRequest,
                user);
        Job savedJob = jobRepository.save(job);
        return buildJobResponseDTO(savedJob);
    }


    public Job getJobById(UUID id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> JobException.jobNotFound(id));
    }

    public JobResponse getJobResponseById(UUID id) {
        Job job = getJobById(id);
        return buildJobResponseDTO(job);
    }


    public Page<JobListResponse> getAllJobsWithFilters(JobFilter filter, Pageable pageable) {
        Page<Job> jobs = jobRepository.findAll(JobSpecification.filterJobs(filter),
                pageable);
        return jobs.map(JobBuilder::buildJobListResponseDTO);
    }
}