package com.zbib.hiresync.controller;

import com.zbib.hiresync.dto.ApplicationCreateRequest;
import com.zbib.hiresync.dto.ApplicationFilter;
import com.zbib.hiresync.dto.ApplicationListResponse;
import com.zbib.hiresync.dto.ApplicationResponse;
import com.zbib.hiresync.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/jobs/{id}")
@RequiredArgsConstructor
public class JobApplicationController {

    private final ApplicationService applicationService;

    @PostMapping("/applicationss")
    public ResponseEntity<ApplicationResponse> createApplication(@Valid @RequestBody ApplicationCreateRequest applicationCreateRequest, @PathVariable UUID id) {
        ApplicationResponse applicationResponse = applicationService.createApplication(applicationCreateRequest, id);
        return ResponseEntity.ok(applicationResponse);
    }

    @GetMapping("/applicationss")
    public ResponseEntity<Page<ApplicationListResponse>> getAllApplications(@PathVariable UUID id, ApplicationFilter filter, Pageable pageable) {
        Page<ApplicationListResponse> applications = applicationService.geJobApplications(id, filter, pageable);
        return ResponseEntity.ok(applications);
    }
}
