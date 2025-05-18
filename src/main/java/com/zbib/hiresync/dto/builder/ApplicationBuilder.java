package com.zbib.hiresync.dto.builder;

import com.zbib.hiresync.dto.request.CreateApplicationRequest;
import com.zbib.hiresync.dto.response.ApplicationResponse;
import com.zbib.hiresync.dto.response.JobApplicationListResponse;
import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.enums.ApplicationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApplicationBuilder {

    public Application buildApplication(CreateApplicationRequest request, Job job) {
        return Application.builder()
                .job(job)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .applicantEmail(request.getEmail())
//                .resumeUrl(request.getResumeUrl())
                .linkedinUrl(request.getLinkedinUrl())
                .status(ApplicationStatus.SUBMITTED)
                .build();
    }

    public JobApplicationListResponse buildJobApplicationListResponse(Application application) {
        return JobApplicationListResponse.builder()
                .id(application.getId())
                .firstName(application.getFirstName())
                .lastName(application.getLastName())
                .applicantEmail(application.getApplicantEmail())
                .matchRate(application.getMatchRate())
                .status(application.getStatus())
                .build();
    }
    
    public ApplicationResponse buildApplicationResponse(Application application) {
        return ApplicationResponse.builder()
                .id(application.getId())
                .firstName(application.getFirstName())
                .lastName(application.getLastName())
                .applicantEmail(application.getApplicantEmail())
                .resumeUrl(application.getResumeUrl())
                .linkedinUrl(application.getLinkedinUrl())
                .status(application.getStatus())
                .notes(application.getNotes())
                .summary(application.getSummary())
                .matchRate(application.getMatchRate())
                .createdAt(application.getCreatedAt())
                .updatedAt(application.getUpdatedAt())
                .build();
    }
}