package com.zbib.hiresync.service;

import static com.zbib.hiresync.builder.JobBuilder.buildJobResponseDTO;

import com.zbib.hiresync.builder.JobBuilder;
import com.zbib.hiresync.dto.JobFilter;
import com.zbib.hiresync.dto.JobListResponse;
import com.zbib.hiresync.dto.JobRequest;
import com.zbib.hiresync.dto.JobResponse;
import com.zbib.hiresync.entity.JobPosting;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.exceptions.JobException;
import com.zbib.hiresync.repository.JobRepository;
import com.zbib.hiresync.specification.JobSpecification;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobService {

  private final JobRepository jobRepository;
  private final UserService userService;

  @Transactional
  public JobResponse createJob(JobRequest jobRequest, UUID userId) {
    User user = userService.getUserById(userId);
    JobPosting job = JobBuilder.buildJobEntity(jobRequest, user);
    JobPosting savedJob = jobRepository.save(job);
    return buildJobResponseDTO(savedJob);
  }

  public JobPosting getJobById(UUID id) {
    return jobRepository.findById(id).orElseThrow(() -> JobException.jobNotFound(id));
  }

  public JobResponse getJobResponseById(UUID id) {
    JobPosting job = getJobById(id);
    return buildJobResponseDTO(job);
  }

  public Page<JobListResponse> getUserJobs(UUID userId, JobFilter filter, Pageable pageable) {
    Page<JobPosting> jobs =
        jobRepository.findAll(JobSpecification.buildSpecification(userId, filter), pageable);
    return jobs.map(JobBuilder::buildJobListResponseDTO);
  }
}
