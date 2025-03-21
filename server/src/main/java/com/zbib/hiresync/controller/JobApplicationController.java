package com.zbib.hiresync.controller;

import com.zbib.hiresync.dto.ApplicationRequest;
import com.zbib.hiresync.dto.ApplicationFilter;
import com.zbib.hiresync.dto.ApplicationListResponse;
import com.zbib.hiresync.dto.ApplicationResponse;
import com.zbib.hiresync.security.UserDetailsImpl;
import com.zbib.hiresync.service.ApplicationService;
import com.zbib.hiresync.validator.JobValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/jobs/{jobId}")
@RequiredArgsConstructor
public class JobApplicationController {

    private final ApplicationService applicationService;
    private final JobValidator jobValidator;

    @PostMapping("/applications")
    public ResponseEntity<ApplicationResponse> createApplication(@Valid @RequestBody ApplicationRequest applicationRequest, @PathVariable UUID jobId) {
        ApplicationResponse applicationResponse = applicationService.createApplication(applicationRequest, jobId);
        return ResponseEntity.ok(applicationResponse);
    }

    @GetMapping("/applications")
    @PreAuthorize("@jobValidator.isJobOwner(#userDetailsImpl, #jobId)")
    public ResponseEntity<Page<ApplicationListResponse>> getAllApplications(
            @AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
            @PathVariable UUID jobId,
            ApplicationFilter filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ApplicationListResponse> applications = applicationService.geJobApplications(jobId, filter, pageable);
        return ResponseEntity.ok(applications);
    }
}
