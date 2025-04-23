package com.zbib.hiresync.dto.builder;

import com.zbib.hiresync.dto.request.CreateApplicationRequest;
import com.zbib.hiresync.dto.response.ApplicationResponse;
import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.enums.ApplicationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Builder for application entities and DTOs
 */
@Component
@RequiredArgsConstructor
public class ApplicationBuilder {

    /**
     * Build an application entity from a create request
     *
     * @param request create request
     * @param job job 
     * @return application entity
     */
    public Application buildApplication(CreateApplicationRequest request, Job job) {
        return Application.builder()
                .job(job)
                .applicantName(request.getApplicantName())
                .applicantEmail(request.getApplicantEmail())
                .phoneNumber(request.getPhoneNumber())
                .coverLetter(request.getCoverLetter())
                .resumeUrl(request.getResumeUrl())
                .portfolioUrl(request.getPortfolioUrl())
                .linkedinUrl(request.getLinkedinUrl())
                .status(ApplicationStatus.SUBMITTED)
                .build();
    }

    /**
     * Build a detailed application response
     *
     * @param application application entity
     * @return application response
     */
    public ApplicationResponse buildApplicationResponse(Application application) {
        Job job = application.getJob();
        
        return ApplicationResponse.builder()
                .id(application.getId())
                .jobId(job.getId())
                .jobTitle(job.getTitle())
                .companyName(job.getCompanyName())
                .applicantName(application.getApplicantName())
                .applicantEmail(application.getApplicantEmail())
                .phoneNumber(application.getPhoneNumber())
                .coverLetter(application.getCoverLetter())
                .resumeUrl(application.getResumeUrl())
                .portfolioUrl(application.getPortfolioUrl())
                .linkedinUrl(application.getLinkedinUrl())
                .status(application.getStatus())
                .notes(application.getNotes())
                .createdAt(application.getCreatedAt())
                .updatedAt(application.getUpdatedAt())
                .build();
    }
} 