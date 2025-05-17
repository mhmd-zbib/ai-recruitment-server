package com.zbib.hiresync.service;

import com.zbib.hiresync.dto.builder.ApplicationBuilder;
import com.zbib.hiresync.dto.filter.ApplicationFilter;
import com.zbib.hiresync.dto.request.CreateApplicationRequest;
import com.zbib.hiresync.dto.request.ToggleApplicationStatusRequest;
import com.zbib.hiresync.dto.response.ApplicationResponse;
import com.zbib.hiresync.dto.response.JobApplicationListResponse;
import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.exception.ApplicationException;
import com.zbib.hiresync.exception.AuthException;
import com.zbib.hiresync.exception.JobException;
import com.zbib.hiresync.repository.ApplicationRepository;
import com.zbib.hiresync.repository.JobRepository;
import com.zbib.hiresync.specification.ApplicationSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final ApplicationBuilder applicationBuilder;
    private final UserService userService;
    private final ApplicationSpecification applicationSpecification;

    @Transactional
    public ApplicationResponse createApplication(CreateApplicationRequest request) {
        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> JobException.notFound(request.getJobId()));

        if (!job.isActive()) {
            throw JobException.notActive(request.getJobId());
        }

        if (applicationRepository.existsByJobAndApplicantEmail(job, request.getApplicantEmail())) {
            throw ApplicationException.alreadyApplied(request.getJobId(), request.getApplicantEmail());
        }

        Application application = applicationBuilder.buildApplication(request, job);
        Application savedApplication = applicationRepository.save(application);

        job.incrementApplicationCount();
        jobRepository.save(job);

        return applicationBuilder.buildApplicationResponse(savedApplication);
    }

    public ApplicationResponse getApplicationById(UUID applicationId, String username) {
        User user = userService.findByUsernameOrThrow(username);
        Application application = findApplicationByIdOrThrow(applicationId);

        if (!application.canBeViewedBy(user)) {
            throw AuthException.accessDenied("application", applicationId, username);
        }

        return applicationBuilder.buildApplicationResponse(application);
    }

    public Page<JobApplicationListResponse> getApplicationsByJobId(UUID jobId, ApplicationFilter filter, Pageable pageable, String username) {
        User user = userService.findByUsernameOrThrow(username);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> JobException.notFound(jobId));

        if (!job.isOwnedBy(user)) {
            throw AuthException.accessDenied("job's applications", jobId, username);
        }

        if (filter == null) {
            filter = new ApplicationFilter();
        }
        filter.setJobId(jobId);

        Specification<Application> spec = applicationSpecification.buildSpecification(filter);
        Page<Application> applications = applicationRepository.findAll(spec, pageable);

        return applications.map(applicationBuilder::buildJobApplicationListResponse);
    }

    @Transactional
    public ApplicationResponse toggleApplicationStatus(UUID applicationId, ToggleApplicationStatusRequest request, String username) {
        User user = userService.findByUsernameOrThrow(username);
        Application application = findApplicationByIdOrThrow(applicationId);

        if (!application.belongsToRecruiter(user)) {
            throw AuthException.accessDenied("application", applicationId, username);
        }

        application.updateStatus(request.getStatus(), request.getNotes());

        Application savedApplication = applicationRepository.save(application);

        return applicationBuilder.buildApplicationResponse(savedApplication);
    }

    private Application findApplicationByIdOrThrow(UUID applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> ApplicationException.notFound(applicationId));
    }
}