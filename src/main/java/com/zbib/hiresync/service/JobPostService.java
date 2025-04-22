package com.zbib.hiresync.service;

import com.zbib.hiresync.dto.builder.JobPostBuilder;
import com.zbib.hiresync.dto.filter.JobPostFilter;
import com.zbib.hiresync.dto.request.CreateJobPostRequest;
import com.zbib.hiresync.dto.request.UpdateJobPostRequest;
import com.zbib.hiresync.dto.response.JobPostResponse;
import com.zbib.hiresync.dto.response.JobPostSummaryResponse;
import com.zbib.hiresync.dto.response.JobPostStatsResponse;
import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.JobPost;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.enums.ApplicationStatus;
import com.zbib.hiresync.exception.ResourceNotFoundException;
import com.zbib.hiresync.exception.auth.UserNotFoundException;
import com.zbib.hiresync.logging.LoggableService;
import com.zbib.hiresync.repository.ApplicationRepository;
import com.zbib.hiresync.repository.JobPostRepository;
import com.zbib.hiresync.repository.UserRepository;
import com.zbib.hiresync.specification.JobPostSpecification;
import com.zbib.hiresync.validator.JobPostValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing job posts
 * Handles CRUD operations, filtering, and authorization for job posts
 */
@Service
@RequiredArgsConstructor
public class JobPostService {

    private final JobPostRepository jobPostRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final JobPostBuilder jobPostBuilder;
    private final JobPostSpecification jobPostSpecification;
    private final JobPostValidator jobPostValidator;
    private final SkillService skillService;
    private final TagService tagService;

    @Transactional
    @LoggableService(message = "Created job post")
    public JobPostResponse createJobPost(CreateJobPostRequest request, String username) {
        User currentUser = findUserByUsernameOrThrow(username);

        JobPost jobPost = jobPostBuilder.buildJobPost(request, currentUser);

        if (request.getSkills() != null && !request.getSkills().isEmpty())
            skillService.addSkillsToJobPost(jobPost, request.getSkills());

        if (request.getTags() != null && !request.getTags().isEmpty())
            tagService.addTagsToJobPost(jobPost, request.getTags());

        JobPost savedJobPost = jobPostRepository.save(jobPost);
        return jobPostBuilder.buildJobPostResponse(savedJobPost);
    }

    @Transactional
    @LoggableService(message = "Updated job post")
    public JobPostResponse updateJobPost(UUID id, UpdateJobPostRequest request, String username) {
        User currentUser = findUserByUsernameOrThrow(username);
        JobPost jobPost = findJobPostByIdOrThrow(id);
        
        jobPostValidator.validateOwnership(jobPost, currentUser);
        
        jobPostBuilder.updateJobPost(jobPost, request);

        if (request.getSkills() != null) {
            jobPost.getSkills().clear();
            skillService.addSkillsToJobPost(jobPost, request.getSkills());
        }

        if (request.getTags() != null) {
            jobPost.getTags().clear();
            tagService.addTagsToJobPost(jobPost, request.getTags());
        }

        JobPost updatedJobPost = jobPostRepository.save(jobPost);
        return jobPostBuilder.buildJobPostResponse(updatedJobPost);
    }

    @Transactional(readOnly = true)
    @LoggableService(message = "Retrieved job post by ID")
    public JobPostResponse getJobPostById(UUID id, String username) {
        User currentUser = findUserByUsernameOrThrow(username);
        JobPost jobPost = findJobPostByIdOrThrow(id);
        
        jobPostValidator.validateJobPostAccess(jobPost, currentUser);

        return jobPostBuilder.buildJobPostResponse(jobPost);
    }
    
    @Transactional(readOnly = true)
    @LoggableService(message = "Retrieved public job post by ID")
    public JobPostResponse getPublicJobPostById(UUID id) {
        JobPost jobPost = findJobPostByIdOrThrow(id);
        
        if (!jobPost.isActive()) {
            throw new ResourceNotFoundException("Job post not found with ID: " + id);
        }

        return jobPostBuilder.buildJobPostResponse(jobPost);
    }

    @Transactional
    @LoggableService(message = "Deleted job post")
    public void deleteJobPost(UUID id, String username) {
        User currentUser = findUserByUsernameOrThrow(username);
        JobPost jobPost = findJobPostByIdOrThrow(id);
        
        jobPostValidator.validateOwnership(jobPost, currentUser);
        
        jobPostRepository.delete(jobPost);
    }

    @Transactional(readOnly = true)
    @LoggableService(message = "Retrieved all job posts with filtering")
    public Page<JobPostSummaryResponse> getAllJobPosts(JobPostFilter filter, Pageable pageable) {
        Specification<JobPost> spec = jobPostSpecification.buildSpecification(filter);
        Page<JobPost> jobPosts = jobPostRepository.findAll(spec, pageable);
        return jobPosts.map(jobPostBuilder::buildJobPostSummaryResponse);
    }
    
    @Transactional(readOnly = true)
    @LoggableService(message = "Retrieved all active public job posts")
    public Page<JobPostSummaryResponse> getAllPublicJobPosts(JobPostFilter filter, Pageable pageable) {
        if (filter == null) {
            filter = new JobPostFilter();
        }
        
        filter.setActive(true);
        filter.setVisibleAfter(LocalDateTime.now());
        
        Specification<JobPost> spec = jobPostSpecification.buildSpecification(filter);
        Page<JobPost> jobPosts = jobPostRepository.findAll(spec, pageable);
        return jobPosts.map(jobPostBuilder::buildJobPostSummaryResponse);
    }
    
    @Transactional(readOnly = true)
    @LoggableService(message = "Retrieved job posts for HR user")
    public Page<JobPostSummaryResponse> getHrJobPosts(String username, JobPostFilter filter, Pageable pageable) {
        User currentUser = findUserByUsernameOrThrow(username);
        
        if (filter == null) {
            filter = new JobPostFilter();
        }
        
        filter.setCreatedById(currentUser.getId());
        
        Specification<JobPost> spec = jobPostSpecification.buildSpecification(filter);
        Page<JobPost> jobPosts = jobPostRepository.findAll(spec, pageable);
        return jobPosts.map(jobPostBuilder::buildJobPostSummaryResponse);
    }

    @Transactional
    @LoggableService(message = "Toggled job post active status")
    public JobPostResponse toggleJobPostActiveStatus(UUID id, String username) {
        User currentUser = findUserByUsernameOrThrow(username);
        JobPost jobPost = findJobPostByIdOrThrow(id);
        
        jobPostValidator.validateOwnership(jobPost, currentUser);
        
        if (jobPost.isActive()) {
            jobPost.deactivate();
        } else {
            jobPost.activate();
        }
        
        JobPost savedJobPost = jobPostRepository.save(jobPost);
        return jobPostBuilder.buildJobPostResponse(savedJobPost);
    }
    
    @Transactional(readOnly = true)
    @LoggableService(message = "Retrieved job post statistics")
    public JobPostStatsResponse getJobPostStats(UUID id, String username) {
        User currentUser = findUserByUsernameOrThrow(username);
        JobPost jobPost = findJobPostByIdOrThrow(id);
        
        jobPostValidator.validateOwnership(jobPost, currentUser);
        
        List<Application> applications = applicationRepository.findByJobPost(jobPost);
        
        long totalApplications = applications.size();
        
        Map<ApplicationStatus, Long> applicationsByStatus = applications.stream()
                .collect(Collectors.groupingBy(Application::getStatus, Collectors.counting()));
        
        return JobPostStatsResponse.builder()
                .jobPostId(jobPost.getId())
                .jobTitle(jobPost.getTitle())
                .totalApplications(totalApplications)
                .applicationsByStatus(applicationsByStatus)
                .build();
    }
    
    @Transactional(readOnly = true)
    @LoggableService(message = "Retrieved job posts expiring soon")
    public List<JobPostSummaryResponse> getJobPostsExpiringSoon(String username, int daysThreshold) {
        User currentUser = findUserByUsernameOrThrow(username);
        
        LocalDateTime thresholdDate = LocalDateTime.now().plusDays(daysThreshold);
        List<JobPost> expiringPosts = jobPostRepository.findByCreatedByAndVisibleUntilBeforeAndActive(
                currentUser, thresholdDate, true);
        
        return expiringPosts.stream()
                .map(jobPostBuilder::buildJobPostSummaryResponse)
                .collect(Collectors.toList());
    }

    private JobPost findJobPostByIdOrThrow(UUID id) {
        return jobPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job post not found with ID: " + id));
    }
    
    private User findUserByUsernameOrThrow(String username) {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + username));
    }
}