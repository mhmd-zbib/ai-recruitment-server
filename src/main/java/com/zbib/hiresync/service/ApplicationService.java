package com.zbib.hiresync.service;

import com.zbib.hiresync.dto.JobCountDTO;
import com.zbib.hiresync.dto.StatusCountDTO;
import com.zbib.hiresync.dto.builder.ApplicationBuilder;
import com.zbib.hiresync.dto.filter.ApplicationFilter;
import com.zbib.hiresync.dto.request.CreateApplicationRequest;
import com.zbib.hiresync.dto.request.UpdateApplicationStatusRequest;
import com.zbib.hiresync.dto.response.ApplicationResponse;
import com.zbib.hiresync.dto.response.ApplicationStatsResponse;
import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.enums.ApplicationStatus;
import com.zbib.hiresync.exception.ResourceNotFoundException;
import com.zbib.hiresync.exception.application.ApplicationAlreadyExistException;
import com.zbib.hiresync.exception.security.UnauthorizedException;
import com.zbib.hiresync.logging.LoggableService;
import com.zbib.hiresync.repository.ApplicationRepository;
import com.zbib.hiresync.repository.JobRepository;
import com.zbib.hiresync.specification.ApplicationSpecification;
import com.zbib.hiresync.validation.ApplicationValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserService userService;
    private final ApplicationBuilder applicationBuilder;
    private final ApplicationSpecification applicationSpecification;
    private final ApplicationValidator applicationValidator;

    @Transactional
    @LoggableService(message = "Created job application")
    public ApplicationResponse createApplication(CreateApplicationRequest request) {
        Job job = findJobByIdOrThrow(request.getJobId());

        if (!job.isActive()) {
            throw new ResourceNotFoundException("Job is not active");
        }

        if (applicationRepository.existsByJobAndApplicantEmail(job, request.getApplicantEmail())) {
            throw new ApplicationAlreadyExistException();
        }

        Application newApplication = applicationBuilder.buildApplication(request, job);
        applicationValidator.validateNewApplication(newApplication);

        // Increment the application count on the job
        job.incrementApplicationCount();
        jobRepository.save(job);

        Application savedApplication = applicationRepository.save(newApplication);

        return applicationBuilder.buildApplicationResponse(savedApplication);
    }

    @Transactional
    @LoggableService(message = "Updated application status")
    public ApplicationResponse updateApplicationStatus(UUID applicationId,
                                                       UpdateApplicationStatusRequest request,
                                                       String username) {
        Application application = findApplicationByIdOrThrow(applicationId);
        User currentUser = userService.findByUsernameOrThrow(username);

        if (!application.belongsToRecruiter(currentUser)) {
            throw UnauthorizedException.updateApplication();
        }

        // Validate status transition before applying
        applicationValidator.validateStatusTransition(application, request.getStatus());
        application.updateStatus(request.getStatus(), request.getNotes());

        Application updatedApplication = applicationRepository.save(application);
        return applicationBuilder.buildApplicationResponse(updatedApplication);
    }

    @Transactional
    @LoggableService(message = "Deleted job application")
    public void deleteApplication(UUID applicationId, String username) {
        Application application = findApplicationByIdOrThrow(applicationId);
        User currentUser = userService.findByUsernameOrThrow(username);

        if (!application.belongsToRecruiter(currentUser)) {
            throw UnauthorizedException.deleteApplication();
        }

        applicationRepository.delete(application);
    }

    @Transactional(readOnly = true)
    @LoggableService(message = "Retrieved application by ID")
    public ApplicationResponse getApplicationById(UUID applicationId, String username) {
        Application application = findApplicationByIdOrThrow(applicationId);
        User currentUser = userService.findByUsernameOrThrow(username);

        if (!application.canBeViewedBy(currentUser)) {
            throw new ResourceNotFoundException("Application not found with ID: " + applicationId);
        }

        return applicationBuilder.buildApplicationResponse(application);
    }

    @Transactional(readOnly = true)
    @LoggableService(message = "Retrieved all applications for user")
    public Page<ApplicationResponse> getAllApplications(ApplicationFilter filter, Pageable pageable, String username) {
        User currentUser = userService.findByUsernameOrThrow(username);

        Specification<Application> spec = applicationSpecification.buildSpecificationForUserJobs(filter, currentUser);
        Page<Application> applications = applicationRepository.findAll(spec, pageable);

        return applications.map(applicationBuilder::buildApplicationResponse);
    }

    @Transactional(readOnly = true)
    @LoggableService(message = "Retrieved applications by status")
    public Page<ApplicationResponse> getApplicationsByStatus(ApplicationStatus status, Pageable pageable, String username) {
        User currentUser = userService.findByUsernameOrThrow(username);

        ApplicationFilter filter = new ApplicationFilter();
        filter.setStatus(status);

        Specification<Application> spec = applicationSpecification.buildSpecificationForUserJobs(filter, currentUser);
        Page<Application> applications = applicationRepository.findAll(spec, pageable);

        return applications.map(applicationBuilder::buildApplicationResponse);
    }

    @Transactional(readOnly = true)
    @LoggableService(message = "Retrieved recent applications")
    public List<ApplicationResponse> getRecentApplications(String username) {
        int limit = 10;
        User currentUser = userService.findByUsernameOrThrow(username);

        List<Application> recentApplications = applicationRepository
                .findRecentApplicationsForCreatedBy(currentUser, limit);

        return recentApplications.stream()
                .map(applicationBuilder::buildApplicationResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @LoggableService(message = "Retrieved application statistics")
    public ApplicationStatsResponse getApplicationStats(String username) {
        User currentUser = userService.findByUsernameOrThrow(username);

        long totalApplications = applicationRepository.countByJobPostCreatedBy(currentUser);

        List<StatusCountDTO> statusCounts = applicationRepository.countApplicationsByStatusForUser(currentUser);
        Map<ApplicationStatus, Long> applicationsByStatus = new HashMap<>();
        for (StatusCountDTO dto : statusCounts) {
            applicationsByStatus.put(dto.getStatus(), dto.getCount());
        }

        List<JobCountDTO> jobCounts = applicationRepository.countApplicationsByJobPostForUser(currentUser);
        Map<UUID, Long> applicationsByJob = new HashMap<>();
        for (JobCountDTO dto : jobCounts) {
            applicationsByJob.put(dto.getJobId(), dto.getCount());
        }

        return ApplicationStatsResponse.builder()
                .totalApplications(totalApplications)
                .applicationsByStatus(applicationsByStatus)
                .applicationsByJob(applicationsByJob)
                .build();
    }

    private Application findApplicationByIdOrThrow(UUID applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + applicationId));
    }

    private Job findJobByIdOrThrow(UUID id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + id));
    }
}