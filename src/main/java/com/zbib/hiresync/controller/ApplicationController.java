package com.zbib.hiresync.controller;

import com.zbib.hiresync.dto.filter.ApplicationFilter;
import com.zbib.hiresync.dto.request.CreateApplicationRequest;
import com.zbib.hiresync.dto.request.UpdateApplicationRequest;
import com.zbib.hiresync.dto.request.UpdateApplicationStatusRequest;
import com.zbib.hiresync.dto.response.ApplicationResponse;
import com.zbib.hiresync.dto.response.ApplicationStatsResponse;
import com.zbib.hiresync.dto.response.ApplicationSummaryResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API controller for job application operations
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Application", description = "Job Application Management")
@RequiredArgsConstructor
@LoggableService(level = LogLevel.INFO)
public class ApplicationController {

    private final ApplicationService applicationService;

    /**
     * Create a new job application (public access)
     */
    @PostMapping("/applications")
    public ResponseEntity<ApplicationResponse> createApplication(
            @Valid @RequestBody CreateApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.createApplication(request));
    }
    
    /**
     * Update an existing application
     */
    @PutMapping("/applications/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApplicationResponse> updateApplication(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateApplicationRequest request,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(applicationService.updateApplication(id, request, username));
    }
    
    /**
     * Update application status
     */
    @PatchMapping("/applications/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApplicationResponse> updateApplicationStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateApplicationStatusRequest request,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(applicationService.updateApplicationStatus(id, request, username));
    }

    /**
     * Delete an application
     */
    @DeleteMapping("/applications/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteApplication(
            @PathVariable UUID id,
            @AuthenticationPrincipal String username) {
        applicationService.deleteApplication(id, username);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get application by ID (for authenticated users)
     */
    @GetMapping("/applications/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApplicationResponse> getApplicationById(
            @PathVariable UUID id,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(applicationService.getApplicationById(id, username));
    }
    
    /**
     * Get application by ID and email (for public access/candidates)
     */
    @GetMapping("/public/applications/{id}")
    public ResponseEntity<ApplicationResponse> getApplicationByIdAndEmail(
            @PathVariable UUID id,
            @RequestParam String email) {
        return ResponseEntity.ok(applicationService.getApplicationByIdAndEmail(id, email));
    }

    /**
     * Get all applications for the HR (with filtering)
     */
    @GetMapping("/applications")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ApplicationSummaryResponse>> getAllApplications(
            ApplicationFilter filter,
            Pageable pageable,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(applicationService.getAllApplications(filter, pageable, username));
    }
    
    /**
     * Get applications by applicant email (public access/candidates)
     */
    @GetMapping("/public/applications")
    public ResponseEntity<Page<ApplicationSummaryResponse>> getApplicationsByEmail(
            @RequestParam String email,
            Pageable pageable) {
        return ResponseEntity.ok(applicationService.getApplicationsByEmail(email, pageable));
    }

    /**
     * Get applications for a specific job post
     */
    @GetMapping("/job-posts/{jobPostId}/applications")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ApplicationSummaryResponse>> getApplicationsByJobPostId(
            @PathVariable UUID jobPostId,
            ApplicationFilter filter,
            Pageable pageable,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(applicationService.getApplicationsByJobPostId(
                jobPostId, filter, pageable, username));
    }
    
    /**
     * Get applications by status
     */
    @GetMapping("/applications/status/{status}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ApplicationSummaryResponse>> getApplicationsByStatus(
            @PathVariable ApplicationStatus status,
            Pageable pageable,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(applicationService.getApplicationsByStatus(status, pageable, username));
    }
    
    /**
     * Get recent applications
     */
    @GetMapping("/applications/recent")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ApplicationSummaryResponse>> getRecentApplications(
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(applicationService.getRecentApplications(username, limit));
    }
    
    /**
     * Get application statistics
     */
    @GetMapping("/applications/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApplicationStatsResponse> getApplicationStatsByHr(
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(applicationService.getApplicationStatsByHr(username));
    }
} 