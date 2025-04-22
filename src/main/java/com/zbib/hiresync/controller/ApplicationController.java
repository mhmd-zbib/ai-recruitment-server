package com.zbib.hiresync.controller;

import com.zbib.hiresync.dto.filter.ApplicationFilter;
import com.zbib.hiresync.dto.request.CreateApplicationRequest;
import com.zbib.hiresync.dto.request.UpdateApplicationRequest;
import com.zbib.hiresync.dto.response.ApplicationResponse;
import com.zbib.hiresync.dto.response.ApplicationSummaryResponse;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/applications")
@Tag(name = "Application", description = "Job Application Management")
@RequiredArgsConstructor
@LoggableService(level = LogLevel.INFO)
public class ApplicationController {

    private final ApplicationService applicationService;

    @Operation(summary = "Submit a new job application", 
        description = "Allows anyone to apply for a job, no authentication required")
    @PostMapping
    public ResponseEntity<ApplicationResponse> createApplication(@Valid @RequestBody CreateApplicationRequest request) {
        ApplicationResponse response = applicationService.createApplication(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Get application by ID", 
        description = "Retrieve a specific application by ID. Requires authentication for recruiters/employers/admins, or must match applicant email")
    @GetMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApplicationResponse> getApplicationById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(applicationService.getApplicationById(id, userDetails.getUsername()));
    }

    @Operation(summary = "Delete an application", 
        description = "Delete an application. Requires proper authorization.")
    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteApplication(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        applicationService.deleteApplication(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all applications with filtering", 
        description = "Retrieve applications with various filters. Regular users can only see their own applications.")
    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Page<ApplicationSummaryResponse>> getAllApplications(
            @Parameter(description = "Filter criteria for applications") ApplicationFilter filter,
            @Parameter(description = "Pagination and sorting parameters. Use page=0&size=10 for pagination. " +
                    "Use sort=fieldName,direction for sorting (e.g. sort=createdAt,desc)")
            Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(applicationService.getAllApplications(filter, pageable, userDetails.getUsername()));
    }

    @Operation(
            summary = "Get all applications for a specific job post",
            description = "Returns all applications submitted to a specific job post with optional filtering. Only the job post creator or admins can access this endpoint."
    )
    @GetMapping("/jobpost/{jobPostId}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Page<ApplicationSummaryResponse>> getApplicationsByJobPost(
            @PathVariable UUID jobPostId,
            @Parameter(description = "Filter criteria for applications") ApplicationFilter filter,
            Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        Page<ApplicationSummaryResponse> applications = applicationService.getApplicationsByJobPostId(
                jobPostId, filter, pageable, userDetails.getUsername());
        return ResponseEntity.ok(applications);
    }
} 