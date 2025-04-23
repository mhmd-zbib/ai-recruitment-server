package com.zbib.hiresync.controller;

import com.zbib.hiresync.dto.filter.ApplicationFilter;
import com.zbib.hiresync.dto.request.CreateApplicationRequest;
import com.zbib.hiresync.dto.request.UpdateApplicationStatusRequest;
import com.zbib.hiresync.dto.response.ApplicationResponse;
import com.zbib.hiresync.enums.ApplicationStatus;
import com.zbib.hiresync.logging.LogLevel;
import com.zbib.hiresync.logging.LoggableService;
import com.zbib.hiresync.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/v1")
@Tag(name = "Application", description = "Job Application Management")
@RequiredArgsConstructor
@LoggableService(level = LogLevel.INFO)
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping("/applications")
    @Operation(summary = "Create a new job application", description = "Creates a new job application with the given details (public access)")
    public ResponseEntity<ApplicationResponse> createApplication(
            @Valid @RequestBody CreateApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.createApplication(request));
    }
    
    /**
     * Update an existing application
     */
    @PutMapping("/applications/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update an existing application", description = "Updates an application with new details. User must be the recruiter who owns the job post.")
    public ResponseEntity<String> updateApplication(
            @Parameter(description = "Application ID") @PathVariable UUID id,
            @Valid @RequestBody Object request,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok("This endpoint has been removed as part of service refactoring");
    }
    
    /**
     * Update application status
     */
    @PatchMapping("/applications/{id}/status")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update application status", description = "Changes the status of an application. User must be the recruiter who owns the job post.")
    public ResponseEntity<ApplicationResponse> updateApplicationStatus(
            @Parameter(description = "Application ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateApplicationStatusRequest request,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(applicationService.updateApplicationStatus(id, request, username));
    }

    /**
     * Delete an application
     */
    @DeleteMapping("/applications/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete an application", description = "Permanently deletes an application. User must be the recruiter who owns the job post.")
    public ResponseEntity<Void> deleteApplication(
            @Parameter(description = "Application ID") @PathVariable UUID id,
            @AuthenticationPrincipal String username) {
        applicationService.deleteApplication(id, username);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get application by ID (for authenticated users)
     */
    @GetMapping("/applications/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get application by ID", description = "Retrieves application details for recruiters. User must be the recruiter who owns the job post or the applicant.")
    public ResponseEntity<ApplicationResponse> getApplicationById(
            @Parameter(description = "Application ID") @PathVariable UUID id,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(applicationService.getApplicationById(id, username));
    }
    
    /**
     * Get application by ID and email (for public access/candidates)
     */
    @GetMapping("/public/applications/{id}")
    @Operation(summary = "Get application by ID and email", description = "Retrieves application details for candidates using email verification")
    public ResponseEntity<String> getApplicationByIdAndEmail(
            @Parameter(description = "Application ID") @PathVariable UUID id,
            @Parameter(description = "Applicant email") @RequestParam String email) {
        return ResponseEntity.ok("This endpoint has been removed as part of service refactoring");
    }

    /**
     * Get all applications for the HR (with filtering)
     */
    @GetMapping("/applications")
    @PreAuthorize("hasAnyRole('RECRUITER', 'EMPLOYER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get all applications", description = "Retrieves all applications for the recruiter with filtering options")
    public ResponseEntity<String> getAllApplications(
            @Parameter(description = "Filter criteria") ApplicationFilter filter,
            @Parameter(description = "Pagination parameters") Pageable pageable,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok("This endpoint has been removed as part of service refactoring");
    }
    
    /**
     * Get applications by applicant email (public access/candidates)
     */
    @GetMapping("/public/applications")
    @Operation(summary = "Get applications by email", description = "Retrieves applications submitted by a specific email address")
    public ResponseEntity<String> getApplicationsByEmail(
            @Parameter(description = "Applicant email") @RequestParam String email,
            @Parameter(description = "Pagination parameters") Pageable pageable) {
        return ResponseEntity.ok("This endpoint has been removed as part of service refactoring");
    }

    /**
     * Get applications for a specific job post
     */
    @GetMapping("/job-posts/{jobPostId}/applications")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get applications for a job post", description = "Retrieves all applications for a specific job post. User must be the recruiter who owns the job post.")
    public ResponseEntity<String> getApplicationsByJobPostId(
            @Parameter(description = "Job Post ID") @PathVariable UUID jobPostId,
            @Parameter(description = "Filter criteria") ApplicationFilter filter,
            @Parameter(description = "Pagination parameters") Pageable pageable,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok("This endpoint has been removed as part of service refactoring");
    }
    
    /**
     * Get applications by status
     */
    @GetMapping("/applications/status/{status}")
    @PreAuthorize("hasAnyRole('RECRUITER', 'EMPLOYER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get applications by status", description = "Retrieves applications filtered by their current status")
    public ResponseEntity<String> getApplicationsByStatus(
            @Parameter(description = "Application status") @PathVariable ApplicationStatus status,
            @Parameter(description = "Pagination parameters") Pageable pageable,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok("This endpoint has been removed as part of service refactoring");
    }
    
    /**
     * Get recent applications
     */
    @GetMapping("/applications/recent")
    @PreAuthorize("hasAnyRole('RECRUITER', 'EMPLOYER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get recent applications", description = "Retrieves most recent applications for the recruiter")
    public ResponseEntity<String> getRecentApplications(
            @Parameter(description = "Maximum number of applications to return") @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok("This endpoint has been removed as part of service refactoring");
    }
    
    /**
     * Get application statistics
     */
    @GetMapping("/applications/stats")
    @PreAuthorize("hasAnyRole('RECRUITER', 'EMPLOYER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get application statistics", description = "Retrieves statistics for applications handled by the recruiter")
    public ResponseEntity<String> getApplicationStatsByHr(
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok("This endpoint has been removed as part of service refactoring");
    }
} 