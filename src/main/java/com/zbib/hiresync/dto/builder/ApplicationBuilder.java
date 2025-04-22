package com.zbib.hiresync.dto.builder;

import com.zbib.hiresync.dto.request.CreateApplicationRequest;
import com.zbib.hiresync.dto.request.UpdateApplicationRequest;
import com.zbib.hiresync.dto.response.ApplicationResponse;
import com.zbib.hiresync.dto.response.ApplicationSummaryResponse;
import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.JobPost;
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
     * @param jobPost job post
     * @return application entity
     */
    public Application buildApplication(CreateApplicationRequest request, JobPost jobPost) {
        return Application.builder()
                .jobPost(jobPost)
                .applicantName(request.getApplicantName())
                .applicantEmail(request.getApplicantEmail())
                .phoneNumber(request.getPhoneNumber())
                .coverLetter(request.getCoverLetter())
                .resumeUrl(request.getResumeUrl())
                .portfolioUrl(request.getPortfolioUrl())
                .linkedinUrl(request.getLinkedinUrl())
                .build();
    }

    /**
     * Update an application entity from an update request
     *
     * @param application existing application
     * @param request update request
     */
    public void updateApplication(Application application, UpdateApplicationRequest request) {
        if (request.getApplicantName() != null) {
            application.setApplicantName(request.getApplicantName());
        }
        
        if (request.getApplicantEmail() != null) {
            application.setApplicantEmail(request.getApplicantEmail());
        }
        
        if (request.getPhoneNumber() != null) {
            application.setPhoneNumber(request.getPhoneNumber());
        }
        
        if (request.getCoverLetter() != null) {
            application.setCoverLetter(request.getCoverLetter());
        }
        
        if (request.getResumeUrl() != null) {
            application.setResumeUrl(request.getResumeUrl());
        }
        
        if (request.getPortfolioUrl() != null) {
            application.setPortfolioUrl(request.getPortfolioUrl());
        }
        
        if (request.getLinkedinUrl() != null) {
            application.setLinkedinUrl(request.getLinkedinUrl());
        }
        
        if (request.getStatus() != null) {
            application.setStatus(request.getStatus());
        }
        
        if (request.getNotes() != null) {
            application.setNotes(request.getNotes());
        }
    }

    /**
     * Build a detailed application response
     *
     * @param application application entity
     * @return application response
     */
    public ApplicationResponse buildApplicationResponse(Application application) {
        JobPost jobPost = application.getJobPost();
        
        return ApplicationResponse.builder()
                .id(application.getId())
                .jobPostId(jobPost.getId())
                .jobTitle(jobPost.getTitle())
                .companyName(jobPost.getCompanyName())
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

    /**
     * Build a summary application response
     *
     * @param application application entity
     * @return application summary response
     */
    public ApplicationSummaryResponse buildApplicationSummaryResponse(Application application) {
        JobPost jobPost = application.getJobPost();
        
        return ApplicationSummaryResponse.builder()
                .id(application.getId())
                .jobPostId(jobPost.getId())
                .jobTitle(jobPost.getTitle())
                .companyName(jobPost.getCompanyName())
                .applicantName(application.getApplicantName())
                .applicantEmail(application.getApplicantEmail())
                .status(application.getStatus())
                .createdAt(application.getCreatedAt())
                .build();
    }
} 