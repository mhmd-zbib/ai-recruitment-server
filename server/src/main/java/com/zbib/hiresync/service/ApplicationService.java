package com.zbib.hiresync.service;

import com.zbib.hiresync.builder.ApplicationBuilder;
import com.zbib.hiresync.dto.ApplicationCreateRequest;
import com.zbib.hiresync.dto.ApplicationFilter;
import com.zbib.hiresync.dto.ApplicationListResponse;
import com.zbib.hiresync.dto.ApplicationResponse;
import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.exceptions.ApplicationException;
import com.zbib.hiresync.repository.ApplicationRepository;
import com.zbib.hiresync.specification.ApplicationSpecification;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.zbib.hiresync.builder.ApplicationBuilder.buildApplication;
import static com.zbib.hiresync.builder.ApplicationBuilder.buildApplicationResponse;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobService jobService;

    public ApplicationResponse createApplication(ApplicationCreateRequest request, UUID jobId) {
        Job job = jobService.getJobById(jobId);
        Application application = buildApplication(request, job);
        Application savedApplication = applicationRepository.save(application);
        return buildApplicationResponse(savedApplication);
    }

    public ApplicationResponse getApplicationById(UUID id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> ApplicationException.applicationNotFound(id));
        return buildApplicationResponse(application);
    }

    public Page<ApplicationListResponse> getAllApplicationsWithFilters(ApplicationFilter filter, Pageable pageable) {
        Page<Application> applications = applicationRepository.findAll(
                ApplicationSpecification.buildSpecification(filter),
                pageable
        );
        return applications.map(ApplicationBuilder::buildApplicationListResponse);
    }
}