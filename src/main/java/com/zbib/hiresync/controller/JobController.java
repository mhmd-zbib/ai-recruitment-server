package com.zbib.hiresync.controller;

import com.zbib.hiresync.dto.filter.ApplicationFilter;
import com.zbib.hiresync.dto.filter.JobFilter;
import com.zbib.hiresync.dto.request.CreateJobRequest;
import com.zbib.hiresync.dto.request.UpdateJobRequest;
import com.zbib.hiresync.dto.response.ApplicationResponse;
import com.zbib.hiresync.dto.response.JobResponse;
import com.zbib.hiresync.dto.response.JobStatsResponse;
import com.zbib.hiresync.dto.response.JobSummaryResponse;
import com.zbib.hiresync.logging.LogLevel;
import com.zbib.hiresync.logging.LoggableService;
import com.zbib.hiresync.service.ApplicationService;
import com.zbib.hiresync.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/jobs")
@Tag(name = "Jobs", description = "Job Listings, Search, and Management")
@RequiredArgsConstructor
@LoggableService(level = LogLevel.INFO)
public class JobController {

    private final JobService jobService;
    private final ApplicationService applicationService;

    @GetMapping("/feed")
    @Operation(summary = "Get all public job posts", description = "Retrieves all active and visible job posts for public access")
    public ResponseEntity<Page<JobSummaryResponse>> getPublicJobs(
            @Parameter(description = "Filter criteria") JobFilter filter,
            @Parameter(description = "Pagination parameters")
            @PageableDefault(size = 10, page = 0) Pageable pageable) {
        return ResponseEntity.ok(jobService.getJobsFeed(filter, pageable));
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a new job", description = "Creates a new job with the given details")
    public ResponseEntity<JobResponse> createJob(
            @Valid @RequestBody CreateJobRequest request,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobService.createJob(request, username));
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update an existing job", description = "Updates a job with new details. User must be the owner.")
    public ResponseEntity<JobResponse> updateJob(
            @Parameter(description = "Job ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateJobRequest request,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(jobService.updateJob(id, request, username));
    }

    @GetMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get job by ID", description = "Retrieves job details for authenticated users")
    public ResponseEntity<JobResponse> getJobById(
            @Parameter(description = "Job ID") @PathVariable UUID id,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(jobService.getJobById(id, username));
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete a job", description = "Permanently deletes a job. User must be the owner.")
    public ResponseEntity<Void> deleteJob(
            @Parameter(description = "Job ID") @PathVariable UUID id,
            @AuthenticationPrincipal String username) {
        jobService.deleteJob(id, username);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get jobs for current user", description = "Retrieves jobs created by the authenticated user with filtering options")
    public ResponseEntity<Page<JobSummaryResponse>> getUserJobs(
            @Parameter(description = "Filter criteria") JobFilter filter,
            @Parameter(description = "Pagination parameters")
            @PageableDefault(size = 10, page = 0) Pageable pageable,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(jobService.getJobs(filter, pageable, username));
    }

    @GetMapping("/{id}/stats")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get job statistics", description = "Retrieves statistics for a specific job. User must be the owner.")
    public ResponseEntity<JobStatsResponse> getJobStats(
            @Parameter(description = "Job ID") @PathVariable UUID id,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(jobService.getJobStats(id, username));
    }

    @GetMapping("/{id}/applications")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get applications for a job post", description = "Retrieves all applications for a specific job post. User must be the recruiter who owns the job post.")
    public ResponseEntity<Page<ApplicationResponse>> getApplicationsByJobPostId(
            @Parameter(description = "Job Post ID") @PathVariable UUID id,
            @Parameter(description = "Filter criteria") ApplicationFilter filter,
            @Parameter(description = "Pagination parameters") Pageable pageable,
            @AuthenticationPrincipal String username) {
        if (filter == null) {
            filter = new ApplicationFilter();
        }
        filter.setJobId(id);
        return ResponseEntity.ok(applicationService.getAllApplications(filter, pageable, username));
    }

    @PatchMapping("/{id}/activate")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Toggle job active status", description = "Activates or deactivates a job. User must be the owner.")
    public ResponseEntity<JobResponse> toggleJobActiveStatus(
            @Parameter(description = "Job ID") @PathVariable UUID id,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(jobService.toggleJobActiveStatus(id, username));
    }

    @PatchMapping("/{id}/extend")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Extend job visibility", description = "Extends the visibility period of a job. User must be the owner.")
    public ResponseEntity<JobResponse> extendJobVisibility(
            @Parameter(description = "Job ID") @PathVariable UUID id,
            @Parameter(description = "Number of days to extend visibility") @RequestParam int days,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(jobService.extendJobVisibility(id, username, days));
    }

} 