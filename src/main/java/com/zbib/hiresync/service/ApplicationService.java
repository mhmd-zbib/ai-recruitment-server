package com.zbib.hiresync.service;

import static com.zbib.hiresync.builder.ApplicationBuilder.buildApplication;
import static com.zbib.hiresync.builder.ApplicationBuilder.buildApplicationResponse;

import com.zbib.hiresync.builder.ApplicationBuilder;
import com.zbib.hiresync.dto.ApplicationFilter;
import com.zbib.hiresync.dto.ApplicationListResponse;
import com.zbib.hiresync.dto.ApplicationRequest;
import com.zbib.hiresync.dto.ApplicationResponse;
import com.zbib.hiresync.dto.JobApplicationFilter;
import com.zbib.hiresync.dto.JobApplicationListResponse;
import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.exceptions.ApplicationException;
import com.zbib.hiresync.repository.ApplicationRepository;
import com.zbib.hiresync.specification.ApplicationSpecification;
import com.zbib.hiresync.specification.JobApplicationSpecification;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class ApplicationService {

  private final ApplicationRepository applicationRepository;
  private final JobService jobService;

  public ApplicationResponse createApplication(ApplicationRequest request, UUID jobId) {
    Job job = jobService.getJobById(jobId);
    Application application = buildApplication(request, job);
    Application savedApplication = applicationRepository.save(application);
    return buildApplicationResponse(savedApplication);
  }

  public ApplicationResponse getApplicationResponseById(UUID id) {
    Application application = getApplicationById(id);
    return buildApplicationResponse(application);
  }

  public Application getApplicationById(UUID id) {
    return applicationRepository
        .findById(id)
        .orElseThrow(() -> ApplicationException.applicationNotFound(id));
  }

  public Page<JobApplicationListResponse> geJobApplications(
      UUID jobId, JobApplicationFilter filter, Pageable pageable) {
    Page<Application> applications =
        applicationRepository.findAll(
            JobApplicationSpecification.buildSpecification(jobId, filter), pageable);
    return applications.map(ApplicationBuilder::buildJobApplicationListResponse);
  }

  public Page<ApplicationListResponse> getApplications(
      UUID userId, ApplicationFilter filter, Pageable pageable) {
    Page<Application> applications =
        applicationRepository.findAll(
            ApplicationSpecification.buildSpecification(userId, filter), pageable);
    return applications.map(ApplicationBuilder::buildApplicationListResponse);
  }

  public void deleteApplicationById(UUID id) {
    Application application = getApplicationById(id);
    applicationRepository.delete(application);
  }
}
