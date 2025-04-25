package com.zbib.hiresync.controller;

import com.zbib.hiresync.dto.filter.ApplicationFilter;
import com.zbib.hiresync.dto.request.CreateApplicationRequest;
import com.zbib.hiresync.dto.request.UpdateApplicationStatusRequest;
import com.zbib.hiresync.dto.response.ApplicationResponse;
import com.zbib.hiresync.dto.response.ApplicationStatsResponse;
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


@RestController
@RequestMapping("/v1/applications")
@Tag(name = "Application", description = "Job Application Management")
@RequiredArgsConstructor
@LoggableService(level = LogLevel.INFO)
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    @Operation(summary = "Create a new job application", description = "Creates a new job application with the given details (public access)")
    public ResponseEntity<ApplicationResponse> createApplication(
            @Valid @RequestBody CreateApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.createApplication(request));
    }

    @PatchMapping("/{id}/status")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update application status", description = "Changes the status of an application. User must be the recruiter who owns the job post.")
    public ResponseEntity<ApplicationResponse> updateApplicationStatus(
            @Parameter(description = "Application ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateApplicationStatusRequest request,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(applicationService.updateApplicationStatus(id, request, username));
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete an application", description = "Permanently deletes an application. User must be the recruiter who owns the job post.")
    public ResponseEntity<Void> deleteApplication(
            @Parameter(description = "Application ID") @PathVariable UUID id,
            @AuthenticationPrincipal String username) {
        applicationService.deleteApplication(id, username);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get application by ID", description = "Retrieves application details for recruiters. User must be the recruiter who owns the job post or the applicant.")
    public ResponseEntity<ApplicationResponse> getApplicationById(
            @Parameter(description = "Application ID") @PathVariable UUID id,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(applicationService.getApplicationById(id, username));
    }

    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get all applications", description = "Retrieves all applications for the recruiter with filtering options")
    public ResponseEntity<Page<ApplicationResponse>> getAllApplications(
            @Parameter(description = "Filter criteria") ApplicationFilter filter,
            @Parameter(description = "Pagination parameters") Pageable pageable,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(applicationService.getAllApplications(filter, pageable, username));
    }

    @GetMapping("/status/{status}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get applications by status", description = "Retrieves applications filtered by their current status")
    public ResponseEntity<Page<ApplicationResponse>> getApplicationsByStatus(
            @Parameter(description = "Application status") @PathVariable ApplicationStatus status,
            @Parameter(description = "Pagination parameters") Pageable pageable,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(applicationService.getApplicationsByStatus(status, pageable, username));
    }

    @GetMapping("/recent")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get recent applications", description = "Retrieves most recent applications for the recruiter")
    public ResponseEntity<List<ApplicationResponse>> getRecentApplications(
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(applicationService.getRecentApplications(username));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('RECRUITER', 'EMPLOYER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get application statistics", description = "Retrieves statistics for applications handled by the recruiter")
    public ResponseEntity<ApplicationStatsResponse> getApplicationStats(
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(applicationService.getApplicationStats(username));
    }
} 