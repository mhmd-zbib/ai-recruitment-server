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
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponse> getApplicationById(@PathVariable UUID id) {
        ApplicationResponse applicationResponse = applicationService.getApplicationById(id);
        return ResponseEntity.ok(applicationResponse);
    }

    @GetMapping
    public ResponseEntity<Page<ApplicationListResponse>> getAllApplications(ApplicationFilter filter, Pageable pageable) {
        Page<ApplicationListResponse> applications = applicationService.getAllApplicationsWithFilters(filter,
                pageable);
        return ResponseEntity.ok(applications);
    }
}