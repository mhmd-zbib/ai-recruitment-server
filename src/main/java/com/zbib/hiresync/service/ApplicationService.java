package com.zbib.hiresync.service;

import com.zbib.hiresync.dto.builder.ApplicationBuilder;
import com.zbib.hiresync.dto.filter.ApplicationFilter;
import com.zbib.hiresync.dto.request.CreateApplicationRequest;
import com.zbib.hiresync.dto.response.ApplicationResponse;
import com.zbib.hiresync.dto.response.ApplicationSummaryResponse;
import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.JobPost;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.exception.application.ApplicationNotFoundException;
import com.zbib.hiresync.exception.auth.UserNotFoundException;
import com.zbib.hiresync.exception.jobpost.JobPostNotFoundException;
import com.zbib.hiresync.logging.LoggableService;
import com.zbib.hiresync.repository.ApplicationRepository;
import com.zbib.hiresync.repository.JobPostRepository;
import com.zbib.hiresync.repository.UserRepository;
import com.zbib.hiresync.specification.ApplicationSpecification;
import com.zbib.hiresync.validator.ApplicationValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for managing job applications
 * Handles CRUD operations, filtering, and authorization for job applications
 */
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobPostRepository jobPostRepository;
    private final UserRepository userRepository;
    private final ApplicationBuilder applicationBuilder;
    private final ApplicationSpecification applicationSpecification;
    private final ApplicationValidator applicationValidator;

    @Transactional
    @LoggableService(message = "Created job application")
    public ApplicationResponse createApplication(CreateApplicationRequest request) {
        JobPost jobPost = findJobPostByIdOrThrow(request.getJobPostId());
        
        applicationValidator.validateApplicationCreation(jobPost, request.getApplicantEmail());

        Application newApplication = applicationBuilder.buildApplication(request, jobPost);
        Application savedApplication = applicationRepository.save(newApplication);
        
        return applicationBuilder.buildApplicationResponse(savedApplication);
    }

    @Transactional
    @LoggableService(message = "Deleted job application")
    public void deleteApplication(UUID applicationId, String username) {
        Application application = findApplicationByIdOrThrow(applicationId);
        User currentUser = findUserByUsernameOrThrow(username);
        
        applicationValidator.validateJobPostApplicationAccess(application.getJobPost(), currentUser);
        
        applicationRepository.delete(application);
    }

    @Transactional(readOnly = true)
    @LoggableService(message = "Retrieved application by ID")
    public ApplicationResponse getApplicationById(UUID applicationId, String username) {
        Application application = findApplicationByIdOrThrow(applicationId);
        User currentUser = findUserByUsernameOrThrow(username);
        
        applicationValidator.validateJobPostApplicationAccess(application.getJobPost(), currentUser);
        
        return applicationBuilder.buildApplicationResponse(application);
    }

    @Transactional(readOnly = true)
    @LoggableService(message = "Retrieved all applications with filtering")
    public Page<ApplicationSummaryResponse> getAllApplications(
            ApplicationFilter filter, 
            Pageable pageable, 
            String username) {
        
        User currentUser = findUserByUsernameOrThrow(username);
        applicationValidator.validateApplicationListAccess(currentUser);

        Specification<Application> userApplicationsSpec = 
            applicationSpecification.buildSpecificationForUserJobPosts(filter, currentUser);
            
        Page<Application> applications = applicationRepository.findAll(userApplicationsSpec, pageable);
        return applications.map(applicationBuilder::buildApplicationSummaryResponse);
    }

    @Transactional(readOnly = true)
    @LoggableService(message = "Retrieved applications for job post")
    public Page<ApplicationSummaryResponse> getApplicationsByJobPostId(
            UUID jobPostId, 
            ApplicationFilter filter, 
            Pageable pageable,
            String username) {
        
        JobPost jobPost = findJobPostByIdOrThrow(jobPostId);
        User currentUser = findUserByUsernameOrThrow(username);
        
        applicationValidator.validateJobPostApplicationAccess(jobPost, currentUser);

        if (filter == null) {
            filter = new ApplicationFilter();
        }
        filter.setJobPostId(jobPostId);
        
        return getAllApplications(filter, pageable, username);
    }

    private Application findApplicationByIdOrThrow(UUID applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
    }

    private JobPost findJobPostByIdOrThrow(UUID id) {
        return jobPostRepository.findById(id)
                .orElseThrow(JobPostNotFoundException::new);
    }
    
    private User findUserByUsernameOrThrow(String username) {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + username));
    }
} 