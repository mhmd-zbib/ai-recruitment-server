package com.zbib.hiresync.controller;

import com.zbib.hiresync.dto.*;
import com.zbib.hiresync.security.UserDetailsImpl;
import com.zbib.hiresync.service.JobService;
import com.zbib.hiresync.validator.JobValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    @PostMapping
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody JobRequest jobRequest, @AuthenticationPrincipal UserDetailsImpl userDetailsImpl) {
        UUID userId = userDetailsImpl.getId();
        JobResponse jobResponse = jobService.createJob(jobRequest, userId);
        return new ResponseEntity<>(jobResponse, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@jobValidator.isJobOwner(#userDetailsImpl, #id)")
    public ResponseEntity<JobResponse> getJobById(@AuthenticationPrincipal UserDetailsImpl userDetailsImpl, @PathVariable UUID id) {
        JobResponse jobResponse = jobService.getJobResponseById(id);
        return ResponseEntity.ok(jobResponse);
    }

    @GetMapping
    public ResponseEntity<Page<JobListResponse>> getAllJobs(@AuthenticationPrincipal UserDetailsImpl userDetailsImpl, JobFilter filter, Pageable pageable) {
        UUID userId = userDetailsImpl != null ? userDetailsImpl.getId() : null;
        Page<JobListResponse> jobListPage = jobService.getUserJobs(userId, filter, pageable);
        return ResponseEntity.ok(jobListPage);
    }
}