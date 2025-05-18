package com.zbib.hiresync.controller;

import com.zbib.hiresync.dto.filter.ApplicationFilter;
import com.zbib.hiresync.dto.filter.JobFilter;
import com.zbib.hiresync.dto.request.CreateApplicationRequest;
import com.zbib.hiresync.dto.request.CreateJobRequest;
import com.zbib.hiresync.dto.request.UpdateJobRequest;
import com.zbib.hiresync.dto.response.*;
import com.zbib.hiresync.service.ApplicationService;
import com.zbib.hiresync.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.PrioritizedParameterNameDiscoverer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/v1/jobs")
@RequiredArgsConstructor
@Tag(name = "Jobs", description = "Job posting management APIs")
public class JobController {

    private final JobService jobService;
    private final ApplicationService applicationService;
    private final PrioritizedParameterNameDiscoverer prioritizedParameterNameDiscoverer;

    @PostMapping
    @Operation(summary = "Create a new job posting", description = "Creates a new job posting with the provided details. The job will be associated with the authenticated user.")
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody CreateJobRequest request, Principal principal) {
        JobResponse jobResponse = jobService.createJob(request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(jobResponse);
    }

    @GetMapping
    @Operation(summary = "Get user's job postings", description = "Returns a paginated list of job postings created by the authenticated user with optional filtering")
    public ResponseEntity<Page<JobListResponse>> getJobs(@ModelAttribute JobFilter filter, Pageable pageable, Principal principal) {
        Page<JobListResponse> jobs = jobService.getJobs(filter, pageable, principal.getName());
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/feed")
    @Operation(summary = "Get public job feed", description = "Returns a paginated list of active job postings for public viewing")
    public ResponseEntity<Page<JobListResponse>> getJobsFeed(@ModelAttribute JobFilter filter, Pageable pageable) {
        Page<JobListResponse> jobs = jobService.getJobsFeed(filter, pageable);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get job by ID", description = "Returns detailed information about a specific job posting")
    public ResponseEntity<JobResponse> getJobById(@PathVariable UUID id) {
        JobResponse job = jobService.getJobById(id);
        return ResponseEntity.ok(job);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a job posting", description = "Updates an existing job posting with the provided details. Only the job creator can update it.")
    public ResponseEntity<JobResponse> updateJob(@PathVariable UUID id, @Valid @RequestBody UpdateJobRequest request, Principal principal) {
        JobResponse updatedJob = jobService.updateJob(id, request, principal.getName());
        return ResponseEntity.ok(updatedJob);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a job posting", description = "Deletes a job posting if it has no applications. Only the job creator can delete it.")
    public ResponseEntity<Void> deleteJob(@PathVariable UUID id, Principal principal) {
        jobService.deleteJob(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/applications")
    @Operation(summary = "Get applications for a job", description = "Returns all applications submitted for a specific job. Only accessible by the job owner.")
    public ResponseEntity<Page<JobApplicationListResponse>> getJobApplications(@PathVariable UUID id, @ModelAttribute ApplicationFilter filter, Pageable pageable,  Principal principal) {
        Page<JobApplicationListResponse> applications = applicationService.getApplicationsByJobId(id, filter, pageable, principal.getName());
        return ResponseEntity.ok(applications);
    }

    @PostMapping("/{jobId}/applications")
    @Operation(
            summary = "Create a new job application",
            description = "Submit an application for a job posting with the applicant's details"
    )
    public ResponseEntity<ApplicationResponse> createApplication(
            @PathVariable UUID jobId,
            @Valid @RequestBody CreateApplicationRequest request) {
        ApplicationResponse response = applicationService.createApplication(jobId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}