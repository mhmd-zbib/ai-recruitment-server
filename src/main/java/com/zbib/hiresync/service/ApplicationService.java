package com.zbib.hiresync.service;

import com.zbib.hiresync.dto.builder.ApplicationBuilder;
import com.zbib.hiresync.dto.request.CreateApplicationRequest;
import com.zbib.hiresync.dto.request.UpdateApplicationStatusRequest;
import com.zbib.hiresync.dto.response.ApplicationResponse;
import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.exception.ResourceNotFoundException;
import com.zbib.hiresync.exception.application.ApplicationAlreadyExistException;
import com.zbib.hiresync.exception.security.UnauthorizedException;
import com.zbib.hiresync.logging.LoggableService;
import com.zbib.hiresync.repository.ApplicationRepository;
import com.zbib.hiresync.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserService userService;
    private final ApplicationBuilder applicationBuilder;

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
        newApplication.validateCompleteness();
        
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

    private Application findApplicationByIdOrThrow(UUID applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + applicationId));
    }

    private Job findJobByIdOrThrow(UUID id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + id));
    }
}