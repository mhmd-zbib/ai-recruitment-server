package com.zbib.hiresync.controller;

import com.zbib.hiresync.dto.filter.ApplicationFilter;
import com.zbib.hiresync.dto.filter.JobPostFilter;
import com.zbib.hiresync.dto.request.CreateJobPostRequest;
import com.zbib.hiresync.dto.request.UpdateJobPostRequest;
import com.zbib.hiresync.dto.response.ApplicationSummaryResponse;
import com.zbib.hiresync.dto.response.JobPostResponse;
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

import java.util.UUID;

@RestController
@RequestMapping("/v1/job-posts")
@Tag(name = "Job Post", description = "Job Post Management")
@RequiredArgsConstructor
@LoggableService(level = LogLevel.INFO)
public class JobPostController {

    private final JobPostService jobPostService;
    private final ApplicationService applicationService;

    @Operation(summary = "Create a new job post")
    @PostMapping
    @PreAuthorize("hasAnyRole('RECRUITER', 'EMPLOYER', 'ADMIN')")
    public ResponseEntity<JobPostResponse> createJobPost(
            @Valid @RequestBody CreateJobPostRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        JobPostResponse response = jobPostService.createJobPost(request, userDetails.getUsername());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Update an existing job post")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECRUITER', 'EMPLOYER', 'ADMIN')")
    public ResponseEntity<JobPostResponse> updateJobPost(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateJobPostRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        JobPostResponse response = jobPostService.updateJobPost(id, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get a job post by ID")
    @GetMapping("/{id}")
    public ResponseEntity<JobPostResponse> getJobPostById(
            @PathVariable UUID id,
            @AuthenticationPrincipal(expression = "username") String username) {
        JobPostResponse response = jobPostService.getJobPostById(id, username);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete a job post")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECRUITER', 'EMPLOYER', 'ADMIN')")
    public ResponseEntity<Void> deleteJobPost(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        jobPostService.deleteJobPost(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all job posts with filtering")
    @GetMapping
    public ResponseEntity<Page<JobPostSummaryResponse>> getAllJobPosts(
            @Parameter(description = "Filter criteria") JobPostFilter filter,
            Pageable pageable) {
        Page<JobPostSummaryResponse> response = jobPostService.getAllJobPosts(filter, pageable);
        return ResponseEntity.ok(response);
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