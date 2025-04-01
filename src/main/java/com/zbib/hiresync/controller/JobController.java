package com.zbib.hiresync.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.zbib.hiresync.dto.JobFilter;
import com.zbib.hiresync.dto.JobListResponse;
import com.zbib.hiresync.dto.JobRequest;
import com.zbib.hiresync.dto.JobResponse;
import com.zbib.hiresync.security.UserDetailsImpl;
import com.zbib.hiresync.service.JobService;

import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;

/**
 * REST controller for managing job resources. Provides endpoints for creating, retrieving, and
 * listing job postings.
 */
@RestController
@RequestMapping("/v1/jobs")
@RequiredArgsConstructor
public class JobController {

  private final JobService jobService;

  /**
   * Creates a new job posting.
   *
   * @param jobRequest the job details to create
   * @param userDetailsImpl the authenticated user creating the job
   * @return the created job with HTTP status 201 (Created)
   */
  @PostMapping
  public ResponseEntity<JobResponse> createJob(
      @Valid @RequestBody JobRequest jobRequest,
      @AuthenticationPrincipal UserDetailsImpl userDetailsImpl) {
    UUID userId = userDetailsImpl.getId();
    JobResponse jobResponse = jobService.createJob(jobRequest, userId);
    return new ResponseEntity<>(jobResponse, HttpStatus.CREATED);
  }

  /**
   * Retrieves a job posting by its ID. Access is restricted to the job owner through PreAuthorize
   * annotation.
   *
   * @param userDetailsImpl the authenticated user
   * @param id the ID of the job to retrieve
   * @return the job posting with HTTP status 200 (OK)
   */
  @GetMapping("/{id}")
  @PreAuthorize("@jobValidator.isJobOwner(#userDetailsImpl, #id)")
  public ResponseEntity<JobResponse> getJobById(
      @AuthenticationPrincipal UserDetailsImpl userDetailsImpl, @PathVariable UUID id) {
    JobResponse jobResponse = jobService.getJobResponseById(id);
    return ResponseEntity.ok(jobResponse);
  }

  /**
   * Retrieves all jobs for the authenticated user with optional filtering and pagination.
   *
   * @param userDetailsImpl the authenticated user
   * @param filter criteria to filter the jobs
   * @param pageable pagination information
   * @return a page of job listings with HTTP status 200 (OK)
   */
  @GetMapping
  public ResponseEntity<Page<JobListResponse>> getAllJobs(
      @AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
      JobFilter filter,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    UUID userId = userDetailsImpl != null ? userDetailsImpl.getId() : null;
    Page<JobListResponse> jobListPage = jobService.getUserJobs(userId, filter, pageable);
    return ResponseEntity.ok(jobListPage);
  }
}
