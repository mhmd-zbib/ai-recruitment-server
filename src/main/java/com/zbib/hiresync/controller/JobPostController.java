package com.zbib.hiresync.controller;

import com.zbib.hiresync.dto.filter.JobPostFilter;
import com.zbib.hiresync.dto.request.CreateJobPostRequest;
import com.zbib.hiresync.dto.request.UpdateJobPostRequest;
import com.zbib.hiresync.dto.response.JobPostResponse;
import com.zbib.hiresync.dto.response.JobPostSummaryResponse;
import com.zbib.hiresync.logging.LogLevel;
import com.zbib.hiresync.logging.LoggableService;
import com.zbib.hiresync.service.JobPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.Set;
import java.util.List;

@RestController
@RequestMapping("/v1/job-posts")
@Tag(name = "Job Post", description = "Job Post Management")
@RequiredArgsConstructor
@LoggableService(level = LogLevel.INFO)
public class JobPostController {

    private final JobPostService jobPostService;

    @Operation(summary = "Create a new job post")
    @PostMapping
    @PreAuthorize("hasAnyRole('RECRUITER', 'EMPLOYER', 'ADMIN')")
    public ResponseEntity<JobPostResponse> createJobPost(@Valid @RequestBody CreateJobPostRequest request) {
        JobPostResponse response = jobPostService.createJobPost(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Update an existing job post")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECRUITER', 'EMPLOYER', 'ADMIN')")
    public ResponseEntity<JobPostResponse> updateJobPost(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateJobPostRequest request) {
        JobPostResponse response = jobPostService.updateJobPost(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get a job post by ID")
    @GetMapping("/{id}")
    public ResponseEntity<JobPostResponse> getJobPostById(@PathVariable UUID id) {
        JobPostResponse response = jobPostService.getJobPostById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete a job post")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECRUITER', 'EMPLOYER', 'ADMIN')")
    public ResponseEntity<Void> deleteJobPost(@PathVariable UUID id) {
        jobPostService.deleteJobPost(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all job posts with comprehensive filtering options", 
        description = "Retrieves job posts with flexible filtering including: unified search " +
                    "across title, description, requirements, and company name, " +
                    "as well as location, skills, tags, salary range, and more. " +
                    "Filter by multiple parameters at once and use Spring's pagination and sorting.")
    @GetMapping
    public ResponseEntity<Page<JobPostSummaryResponse>> getAllJobPosts(
            @Parameter(description = "Filter criteria for job posts") JobPostFilter filter,
            @Parameter(description = "Pagination and sorting parameters. Use page=0&size=10 for pagination. " +
                    "Use sort=fieldName,direction for sorting (e.g. sort=createdAt,desc). " +
                    "Multiple sort criteria can be used with multiple sort parameters (e.g. sort=salary,desc&sort=title,asc)")
            Pageable pageable) {
        return ResponseEntity.ok(jobPostService.getAllJobPosts(filter, pageable));
    }
} 