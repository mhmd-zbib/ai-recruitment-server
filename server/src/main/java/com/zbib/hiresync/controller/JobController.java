package com.zbib.hiresync.controller;

import com.zbib.hiresync.dto.*;
import com.zbib.hiresync.security.JwtUtil;
import com.zbib.hiresync.security.UserDetailsImpl;
import com.zbib.hiresync.service.ApplicationService;
import com.zbib.hiresync.service.JobService;
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
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final ApplicationService applicationService;

    @PostMapping
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody JobRequest jobRequest, @AuthenticationPrincipal UserDetailsImpl userDetailsImpl) {
        Long userId = userDetailsImpl.getId();
        JobResponse jobResponse = jobService.createJob(jobRequest,userId);
        return new ResponseEntity<>(jobResponse,
                HttpStatus.CREATED);
    }

    @PostMapping("/{id}")
    public ResponseEntity<ApplicationResponse> createApplication(@Valid @RequestBody ApplicationCreateRequest applicationCreateRequest, @PathVariable UUID id) {
        ApplicationResponse applicationResponse = applicationService.createApplication(applicationCreateRequest,id);
        return ResponseEntity.ok(applicationResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJobById(@PathVariable UUID id) {
        JobResponse jobResponse = jobService.getJobResponseById(id);
        return ResponseEntity.ok(jobResponse);
    }

    @GetMapping
    public ResponseEntity<Page<JobListResponse>> getAllJobs(@AuthenticationPrincipal UserDetailsImpl userDetailsImpl, JobFilter filter, Pageable pageable) {

        Long userId = userDetailsImpl != null ? userDetailsImpl.getId() : null;
        filter.setUserId(userId);

        Page<JobListResponse> jobListPage = jobService.getAllJobsWithFilters(filter,
                pageable);
        return ResponseEntity.ok(jobListPage);
    }
}