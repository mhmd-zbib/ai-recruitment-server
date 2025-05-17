package com.zbib.hiresync.controller;

import com.zbib.hiresync.dto.filter.ApplicationFilter;
import com.zbib.hiresync.dto.filter.JobFilter;
import com.zbib.hiresync.dto.request.CreateJobRequest;
import com.zbib.hiresync.dto.request.UpdateJobRequest;
import com.zbib.hiresync.dto.response.ApplicationResponse;
import com.zbib.hiresync.dto.response.JobApplicationListResponse;
import com.zbib.hiresync.dto.response.JobResponse;
import com.zbib.hiresync.dto.response.JobListResponse;
import com.zbib.hiresync.service.ApplicationService;
import com.zbib.hiresync.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/jobs")
@RequiredArgsConstructor
@Tag(name = "Jobs", description = "Job posting management APIs")
public class JobController {

    private final JobService jobService;
    private final ApplicationService applicationService;

    @PostMapping
    @Operation(
        summary = "Create a new job posting",
        description = "Creates a new job posting with the provided details. The job will be associated with the authenticated user."
    )
    public ResponseEntity<JobResponse> createJob(
            @Valid @RequestBody CreateJobRequest request,
            @AuthenticationPrincipal String username) {
        JobResponse jobResponse = jobService.createJob(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(jobResponse);
    }
    
    @GetMapping
    @Operation(
        summary = "Get user's job postings",
        description = "Returns a paginated list of job postings created by the authenticated user with optional filtering"
    )
    public ResponseEntity<Page<JobListResponse>> getJobs(
            @ModelAttribute JobFilter filter,
            Pageable pageable,
            @AuthenticationPrincipal String username) {
        Page<JobListResponse> jobs = jobService.getJobs(filter, pageable, username);
        return ResponseEntity.ok(jobs);
    }
    
    @GetMapping("/feed")
    @Operation(
        summary = "Get public job feed",
        description = "Returns a paginated list of active job postings for public viewing"
    )
    public ResponseEntity<Page<JobListResponse>> getJobsFeed(
            @ModelAttribute JobFilter filter,
            Pageable pageable) {
        Page<JobListResponse> jobs = jobService.getJobsFeed(filter, pageable);
        return ResponseEntity.ok(jobs);
    }

//    @GetMapping("/{id}")
//    @Operation(
//        summary = "Get job by ID",
//        description = "Returns detailed information about a specific job posting"
//    )
//    public ResponseEntity<JobResponse> getJobById(
//            @PathVariable UUID id,
//            @RequestParam(required = false, defaultValue = "true") boolean detailed,
//            @AuthenticationPrincipal String username) {
//        JobResponse job = jobService.getJobById(id, username, detailed);
//        return ResponseEntity.ok(job);
//    }
    
    @PutMapping("/{id}")
    @Operation(
        summary = "Update a job posting",
        description = "Updates an existing job posting with the provided details. Only the job creator can update it."
    )
    public ResponseEntity<JobResponse> updateJob(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateJobRequest request,
            @AuthenticationPrincipal String username) {
        JobResponse updatedJob = jobService.updateJob(id, request, username);
        return ResponseEntity.ok(updatedJob);
    }
    
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete a job posting",
        description = "Deletes a job posting if it has no applications. Only the job creator can delete it."
    )
    public ResponseEntity<Void> deleteJob(
            @PathVariable UUID id,
            @AuthenticationPrincipal String username) {
        jobService.deleteJob(id, username);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{id}/applications")
    @Operation(
        summary = "Get applications for a job",
        description = "Returns all applications submitted for a specific job. Only accessible by the job owner."
    )
    public ResponseEntity<Page<JobApplicationListResponse>> getJobApplications(
            @PathVariable UUID id,
            @ModelAttribute ApplicationFilter filter,
            Pageable pageable,
            @AuthenticationPrincipal String username) {
        Page<JobApplicationListResponse> applications = applicationService.getApplicationsByJobId(id, filter, pageable, username);
        return ResponseEntity.ok(applications);
    }
}