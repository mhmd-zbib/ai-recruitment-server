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
import com.zbib.hiresync.exception.UnauthorizedException;
import com.zbib.hiresync.logging.LoggableService;
import com.zbib.hiresync.repository.JobPostRepository;
import com.zbib.hiresync.specification.JobPostSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobPostService {

    private final JobPostRepository jobPostRepository;
    private final JobPostBuilder jobPostBuilder;
    private final JobPostSpecification jobPostSpecification;
    private final SkillService skillService;
    private final TagService tagService;
    private final AuthService authService;

    @Transactional
    @LoggableService(message = "Created job post")
    public JobPostResponse createJobPost(CreateJobPostRequest request) {
        User currentUser = authService.getCurrentUser();

        JobPost jobPost = jobPostBuilder.buildJobPost(request, currentUser);

        if (request.getSkills() != null && !request.getSkills().isEmpty())
            skillService.addSkillsToJobPost(jobPost, request.getSkills());

        if (request.getTags() != null && !request.getTags().isEmpty())
            tagService.addTagsToJobPost(jobPost, request.getTags());

        JobPost savedJobPost = jobPostRepository.save(jobPost);
        return jobPostBuilder.buildJobPostResponse(savedJobPost);
    }

    @Transactional
    public JobPostResponse updateJobPost(UUID id, UpdateJobPostRequest request) {
        User currentUser = authService.getCurrentUser();
        JobPost jobPost = findJobPostAndVerifyOwnership(id, currentUser);

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
    public JobPostResponse getJobPostById(UUID id) {
        User currentUser = authService.getCurrentUser();
        JobPost jobPost = jobPostRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Job post not found with ID: " + id));
        if (!jobPost.isActive() && !jobPost.getCreatedBy().getId().equals(currentUser.getId()))
            throw new ResourceNotFoundException("Job post not found with ID: " + id);

        return jobPostBuilder.buildJobPostResponse(jobPost);
    }

    @Transactional
    public void deleteJobPost(UUID id) {
        User currentUser = authService.getCurrentUser();
        JobPost jobPost = findJobPostAndVerifyOwnership(id, currentUser);
        jobPostRepository.delete(jobPost);
    }

    @Transactional(readOnly = true)
    public Page<JobPostSummaryResponse> getAllJobPosts(JobPostFilter filter, Pageable pageable) {
        Specification<JobPost> spec = jobPostSpecification.buildSpecification(filter);
        Page<JobPost> jobPosts = jobPostRepository.findAll(spec, pageable);
        return jobPosts.map(jobPostBuilder::buildJobPostSummaryResponse);
    }

    private JobPost findJobPostAndVerifyOwnership(UUID id, User user) {
        JobPost jobPost = jobPostRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Job post not found with ID: " + id));
        if (!jobPost.getCreatedBy().getId().equals(user.getId()))
            throw new UnauthorizedException("You are not authorized to modify this job post");
        return jobPost;
    }
}