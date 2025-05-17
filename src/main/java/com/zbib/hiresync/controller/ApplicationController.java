package com.zbib.hiresync.controller;

import com.zbib.hiresync.dto.request.CreateApplicationRequest;
import com.zbib.hiresync.dto.request.ToggleApplicationStatusRequest;
import com.zbib.hiresync.dto.response.ApplicationResponse;
import com.zbib.hiresync.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/v1/applications")
@RequiredArgsConstructor
@Tag(name = "Applications", description = "Job application management APIs")
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    @Operation(
        summary = "Create a new job application",
        description = "Submit an application for a job posting with the applicant's details"
    )
    public ResponseEntity<ApplicationResponse> createApplication(
            @Valid @RequestBody CreateApplicationRequest request) {
        ApplicationResponse response = applicationService.createApplication(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get application by ID",
        description = "Retrieve detailed information about a specific application. Only accessible by the job creator or the applicant."
    )
    public ResponseEntity<ApplicationResponse> getApplicationById(
            @PathVariable UUID id,
            Principal principal) {
        ApplicationResponse response = applicationService.getApplicationById(id, principal.getName());
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}/status")
    @Operation(
        summary = "Update application status",
        description = "Change the status of an application (e.g., to SHORTLISTED, INTERVIEW_SCHEDULED, etc). Only accessible by the job creator."
    )
    public ResponseEntity<ApplicationResponse> toggleApplicationStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ToggleApplicationStatusRequest request,
            Principal principal) {
        ApplicationResponse response = applicationService.toggleApplicationStatus(id, request, principal.getName());
        return ResponseEntity.ok(response);
    }
}