package com.zbib.hiresync.controller;

import com.zbib.hiresync.dto.JobFilter;
import com.zbib.hiresync.dto.JobListResponseDTO;
import com.zbib.hiresync.dto.JobRequestDTO;
import com.zbib.hiresync.dto.JobResponseDTO;
import com.zbib.hiresync.enums.EmploymentType;
import com.zbib.hiresync.enums.JobStatus;
import com.zbib.hiresync.enums.LocationType;
import com.zbib.hiresync.security.JwtUtil;
import com.zbib.hiresync.security.UserDetailsImpl;
import com.zbib.hiresync.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final JwtUtil jwtUtil;

    @PostMapping
    public ResponseEntity<JobResponseDTO> createJob(
            @Valid @RequestBody JobRequestDTO jobRequestDTO,
            @AuthenticationPrincipal UserDetailsImpl userDetailsImpl) {

        Long userId = userDetailsImpl.getId();

        JobResponseDTO jobResponseDTO = jobService.createJob(jobRequestDTO,
                userId);
        return new ResponseEntity<>(jobResponseDTO,
                HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponseDTO> getJobById(@PathVariable UUID id) {
        JobResponseDTO jobResponseDTO = jobService.getJobById(id);
        return ResponseEntity.ok(jobResponseDTO);
    }

    @GetMapping
    public ResponseEntity<Page<JobListResponseDTO>> getAllJobs(
            @AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
            JobFilter filter,
            Pageable pageable) {

        Long userId = userDetailsImpl != null ? userDetailsImpl.getId() : null;
        filter.setUserId(userId);

        Page<JobListResponseDTO> jobListPage = jobService.getAllJobsWithFilters(filter, pageable);
        return ResponseEntity.ok(jobListPage);
    }
}