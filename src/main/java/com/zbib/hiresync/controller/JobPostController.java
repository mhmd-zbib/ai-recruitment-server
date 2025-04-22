package com.zbib.hiresync.controller;

import com.zbib.hiresync.dto.filter.ApplicationFilter;
import com.zbib.hiresync.dto.filter.JobPostFilter;
import com.zbib.hiresync.dto.request.CreateJobPostRequest;
import com.zbib.hiresync.dto.request.UpdateJobPostRequest;
import com.zbib.hiresync.dto.response.ApplicationSummaryResponse;
import com.zbib.hiresync.dto.response.JobPostResponse;
import com.zbib.hiresync.dto.response.JobPostStatsResponse;
import com.zbib.hiresync.dto.response.JobPostSummaryResponse;
import com.zbib.hiresync.logging.LogLevel;
import com.zbib.hiresync.logging.LoggableService;
import com.zbib.hiresync.service.ApplicationService;
import com.zbib.hiresync.service.JobPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API controller for job post operations
 */
@RestController
@RequestMapping("/api/v1/job-posts")
@Tag(name = "Job Post", description = "Job Post Management")
@RequiredArgsConstructor
@LoggableService(level = LogLevel.INFO)
public class JobPostController {

    private final JobPostService jobPostService;
    private final ApplicationService applicationService;

    /**
     * Create a new job post
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JobPostResponse> createJobPost(
            @Valid @RequestBody CreateJobPostRequest request,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobPostService.createJobPost(request, username));
    }

    /**
     * Update an existing job post
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JobPostResponse> updateJobPost(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateJobPostRequest request,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(jobPostService.updateJobPost(id, request, username));
    }

    /**
     * Get a job post by ID (for authenticated users)
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JobPostResponse> getJobPostById(
            @PathVariable UUID id,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(jobPostService.getJobPostById(id, username));
    }
    
    /**
     * Get a job post by ID (public access)
     */
    @GetMapping("/public/{id}")
    public ResponseEntity<JobPostResponse> getPublicJobPostById(@PathVariable UUID id) {
        return ResponseEntity.ok(jobPostService.getPublicJobPostById(id));
    }

    /**
     * Delete a job post
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteJobPost(
            @PathVariable UUID id,
            @AuthenticationPrincipal String username) {
        jobPostService.deleteJobPost(id, username);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get all job posts with filtering options
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<JobPostSummaryResponse>> getAllJobPosts(
            JobPostFilter filter,
            Pageable pageable,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(jobPostService.getHrJobPosts(username, filter, pageable));
    }
    
    /**
     * Get all public job posts
     */
    @GetMapping("/public")
    public ResponseEntity<Page<JobPostSummaryResponse>> getAllPublicJobPosts(
            JobPostFilter filter,
            Pageable pageable) {
        return ResponseEntity.ok(jobPostService.getAllPublicJobPosts(filter, pageable));
    }
    
    /**
     * Toggle job post active status
     */
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JobPostResponse> toggleJobPostStatus(
            @PathVariable UUID id,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(jobPostService.toggleJobPostActiveStatus(id, username));
    }
    
    /**
     * Get job post statistics
     */
    @GetMapping("/{id}/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JobPostStatsResponse> getJobPostStats(
            @PathVariable UUID id,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(jobPostService.getJobPostStats(id, username));
    }
    
    /**
     * Get job posts expiring soon
     */
    @GetMapping("/expiring-soon")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<JobPostSummaryResponse>> getJobPostsExpiringSoon(
            @RequestParam(defaultValue = "7") int days,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(jobPostService.getJobPostsExpiringSoon(username, days));
    }

    @Operation(summary = "Get applications for a job post")
    @GetMapping("/{id}/applications")
    @PreAuthorize("hasAnyRole('RECRUITER', 'EMPLOYER', 'ADMIN')")
    public ResponseEntity<Page<ApplicationSummaryResponse>> getJobPostApplications(
            @PathVariable("id") UUID jobPostId,
            ApplicationFilter filter,
            Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        Page<ApplicationSummaryResponse> applications = applicationService.getApplicationsByJobPostId(
                jobPostId, filter, pageable, userDetails.getUsername());
        return ResponseEntity.ok(applications);
    }
} 