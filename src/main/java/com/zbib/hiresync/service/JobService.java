package com.zbib.hiresync.service;

import static com.zbib.hiresync.builder.JobBuilder.buildJobResponseDTO;

import com.zbib.hiresync.builder.JobBuilder;
import com.zbib.hiresync.dto.JobFilter;
import com.zbib.hiresync.dto.JobListResponse;
import com.zbib.hiresync.dto.JobRequest;
import com.zbib.hiresync.dto.JobResponse;
import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.exceptions.JobException;
import com.zbib.hiresync.repository.JobRepository;
import com.zbib.hiresync.specification.JobSpecification;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobService {

  private final JobRepository jobRepository;
  private final UserService userService;

  public JobResponse createJob(JobRequest jobRequest, UUID userId) {
    User user = userService.getUserById(userId);
    Job job = JobBuilder.buildJobEntity(jobRequest, user);
    Job savedJob = jobRepository.save(job);
    return buildJobResponseDTO(savedJob);
  }

  public Job getJobById(UUID id) {
    return jobRepository.findById(id).orElseThrow(() -> JobException.jobNotFound(id));
  }

  public JobResponse getJobResponseById(UUID id) {
    Job job = getJobById(id);
    return buildJobResponseDTO(job);
  }

  public Page<JobListResponse> getUserJobs(UUID userId, JobFilter filter, Pageable pageable) {
    Page<Job> jobs =
        jobRepository.findAll(JobSpecification.buildSpecification(userId, filter), pageable);
    return jobs.map(JobBuilder::buildJobListResponseDTO);
  }
}
