package com.zbib.hiresync.service;

import com.zbib.hiresync.dto.ApplicationRequest;
import com.zbib.hiresync.dto.ApplicationResponse;
import com.zbib.hiresync.dto.ApplicationFilter;
import com.zbib.hiresync.dto.JobApplicationFilter;
import com.zbib.hiresync.dto.ApplicationListResponse;
import com.zbib.hiresync.dto.JobApplicationListResponse;
import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.JobPosting;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.enums.ApplicationStatus;
import com.zbib.hiresync.exceptions.ApplicationException;
import com.zbib.hiresync.repository.ApplicationRepository;
import com.zbib.hiresync.repository.JobRepository;
import com.zbib.hiresync.repository.UserRepository;
import com.zbib.hiresync.specification.ApplicationSpecification;
import com.zbib.hiresync.specification.JobApplicationSpecification;
import com.zbib.hiresync.validator.ApplicationValidator;
import com.zbib.hiresync.builder.ApplicationBuilder;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service class for managing job applications.
 */
@Service
@RequiredArgsConstructor
public final class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ApplicationValidator applicationValidator;

    /**
     * Creates a new job application.
     *
     * @param request the application request
     * @param userId the user ID
     * @param jobId the job ID
     * @return the created application response
     */
    @Transactional
    public ApplicationResponse createApplication(
            final ApplicationRequest request,
            final UUID userId,
            final UUID jobId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(
                        HttpStatus.NOT_FOUND,
                        "User not found",
                        "User with id " + userId + " does not exist"));
        JobPosting job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ApplicationException(
                        HttpStatus.NOT_FOUND,
                        "Job not found",
                        "Job with id " + jobId + " does not exist"));

        Application application = Application.builder()
                .user(user)
                .job(job)
                .status(ApplicationStatus.SUBMITTED)
                .build();

        return ApplicationResponse.builder()
                .id(applicationRepository.save(application).getId())
                .status(application.getStatus())
                .build();
    }

    /**
     * Gets an application response by ID.
     *
     * @param id the application ID
     * @return the application response
     */
    public ApplicationResponse getApplicationResponseById(final UUID id) {
        return applicationRepository.findById(id)
            .map(application -> ApplicationResponse.builder()
                .id(application.getId())
                .jobId(application.getJob().getId())
                .firstName(application.getFirstName())
                .lastName(application.getLastName())
                .email(application.getEmail())
                .phoneNumber(application.getPhoneNumber())
                .linkedInUrl(application.getLinkedinUrl())
                .websiteUrl(application.getWebsiteUrl())
                .cvUrl(application.getCvUrl())
                .status(application.getStatus())
                .referredBy(application.getReferredBy())
                .appliedAt(application.getCreatedAt())
                .build())
            .orElseThrow(() -> ApplicationException.applicationNotFound(id));
    }

    /**
     * Gets an application by ID.
     *
     * @param id the application ID
     * @return the application
     */
    public Application getApplicationById(final UUID id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(
                    HttpStatus.NOT_FOUND,
                    "Application not found",
                    "Application with id " + id + " does not exist"));
    }

    /**
     * Gets all applications for a job.
     *
     * @param jobId the job ID
     * @param filter the filter criteria
     * @param pageable the pagination information
     * @return a page of application responses
     */
    public Page<JobApplicationListResponse> getJobApplications(
            final UUID jobId,
            final JobApplicationFilter filter,
            final Pageable pageable) {
        return applicationRepository.findAll(
            JobApplicationSpecification.buildSpecification(jobId, filter),
            pageable)
            .map(ApplicationBuilder::buildJobApplicationListResponse);
    }

    /**
     * Gets all applications for a user.
     *
     * @param userId the user ID
     * @param filter the filter criteria
     * @param pageable the pagination information
     * @return a page of application responses
     */
    public Page<ApplicationListResponse> getApplications(
            final UUID userId,
            final ApplicationFilter filter,
            final Pageable pageable) {
        return applicationRepository.findAll(
            ApplicationSpecification.buildSpecification(userId, filter),
            pageable)
            .map(ApplicationBuilder::buildApplicationListResponse);
    }

    /**
     * Deletes an application by ID.
     *
     * @param id the application ID
     */
    @Transactional
    public void deleteApplicationById(final UUID id) {
        Application application = applicationRepository.findById(id)
            .orElseThrow(() -> new ApplicationException(
                HttpStatus.NOT_FOUND,
                "Application not found",
                "Application with id " + id + " does not exist"));
        applicationRepository.delete(application);
    }
}
