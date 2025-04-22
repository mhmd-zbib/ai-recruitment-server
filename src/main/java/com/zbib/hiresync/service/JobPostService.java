package com.zbib.hiresync.service;

import com.zbib.hiresync.dto.builder.JobPostBuilder;
import com.zbib.hiresync.dto.filter.JobPostFilter;
import com.zbib.hiresync.dto.request.CreateJobPostRequest;
import com.zbib.hiresync.dto.request.UpdateJobPostRequest;
import com.zbib.hiresync.dto.response.JobPostResponse;
import com.zbib.hiresync.dto.response.JobPostSummaryResponse;
import com.zbib.hiresync.entity.JobPost;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.exception.ResourceNotFoundException;
import com.zbib.hiresync.exception.auth.UserNotFoundException;
import com.zbib.hiresync.logging.LoggableService;
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

import java.util.UUID;

/**
 * Service for managing job posts
 * Handles CRUD operations, filtering, and authorization for job posts
 */
@Service
@RequiredArgsConstructor
public class JobPostService {

    private final JobPostRepository jobPostRepository;
    private final UserRepository userRepository;
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

    private JobPost findJobPostByIdOrThrow(UUID id) {
        return jobPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job post not found with ID: " + id));
    }
    
    private User findUserByUsernameOrThrow(String username) {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + username));
    }
}