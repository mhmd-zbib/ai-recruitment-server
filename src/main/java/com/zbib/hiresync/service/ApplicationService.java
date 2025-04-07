package com.zbib.hiresync.service;

import static com.zbib.hiresync.builder.ApplicationBuilder.buildApplication;
import static com.zbib.hiresync.builder.ApplicationBuilder.buildApplicationResponse;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zbib.hiresync.builder.ApplicationBuilder;
import com.zbib.hiresync.dto.ApplicationFilter;
import com.zbib.hiresync.dto.ApplicationListResponse;
import com.zbib.hiresync.dto.ApplicationRequest;
import com.zbib.hiresync.dto.ApplicationResponse;
import com.zbib.hiresync.dto.JobApplicationFilter;
import com.zbib.hiresync.dto.JobApplicationListResponse;
import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.JobPosting;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.exceptions.ApplicationException;
import com.zbib.hiresync.repository.ApplicationRepository;
import com.zbib.hiresync.specification.ApplicationSpecification;
import com.zbib.hiresync.specification.JobApplicationSpecification;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationService {

  private final ApplicationRepository applicationRepository;
  private final JobService jobService;
  private final UserService userService;
  private final ApplicationBuilder applicationBuilder;

  @Transactional
  public ApplicationResponse createApplication(ApplicationRequest request, UUID userId, Long jobId) {
    JobPosting job = jobService.getJobById(jobId);
    User user = userService.getUserById(userId);
    Application application = applicationBuilder.buildApplication(request, user, job);
    Application savedApplication = applicationRepository.save(application);
    return applicationBuilder.buildApplicationResponse(savedApplication);
  }

  public ApplicationResponse getApplicationById(UUID id) {
    Application application = applicationRepository.findById(id)
            .orElseThrow(() -> ApplicationException.applicationNotFound(id));
    return applicationBuilder.buildApplicationResponse(application);
  }

  public Page<JobApplicationListResponse> geJobApplications(
      UUID jobId, JobApplicationFilter filter, Pageable pageable) {
    Page<Application> applications =
        applicationRepository.findAll(
            JobApplicationSpecification.buildSpecification(jobId, filter), pageable);
    return applications.map(ApplicationBuilder::buildJobApplicationListResponse);
  }

  public Page<ApplicationListResponse> getUserApplications(UUID userId, ApplicationFilter filter, Pageable pageable) {
    Page<Application> applications = applicationRepository.findAll(
            ApplicationSpecification.buildSpecification(userId, filter), pageable);
    return applications.map(applicationBuilder::buildApplicationListResponse);
  }

  public void deleteApplicationById(UUID id) {
    Application application = getApplicationById(id);
    applicationRepository.delete(application);
  }
}
