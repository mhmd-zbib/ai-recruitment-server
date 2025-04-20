package com.zbib.hiresync.dto.builder;

import com.zbib.hiresync.dto.request.CreateJobPostRequest;
import com.zbib.hiresync.dto.request.UpdateJobPostRequest;
import com.zbib.hiresync.dto.response.JobPostResponse;
import com.zbib.hiresync.dto.response.JobPostSummaryResponse;
import com.zbib.hiresync.entity.Address;
import com.zbib.hiresync.entity.JobPost;
import com.zbib.hiresync.entity.Skill;
import com.zbib.hiresync.entity.Tag;
import com.zbib.hiresync.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builder for JobPost DTOs and entities
 */
@Component
@RequiredArgsConstructor
public class JobPostBuilder {

    /**
     * Build a JobPost entity from a CreateJobPostRequest
     */
    public JobPost buildJobPost(CreateJobPostRequest request, User createdBy) {
        Address address = null;
        if (request.getLocation() != null && !request.getLocation().isEmpty()) {
            address = Address.builder()
                    .formattedAddress(request.getLocation())
                    .build();
        }
        
        return JobPost.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .requirements(request.getRequirements())
                .companyName(request.getCompanyName())
                .address(address)
                .workplaceType(request.getWorkplaceType())
                .employmentType(request.getEmploymentType())
                .minSalary(request.getMinSalary())
                .maxSalary(request.getMaxSalary())
                .currency(request.getCurrency())
                .active(request.isActive())
                .visibleUntil(request.getVisibleUntil())
                .createdBy(createdBy)
                .build();
    }

    /**
     * Update a JobPost entity from an UpdateJobPostRequest
     */
    public JobPost updateJobPost(JobPost jobPost, UpdateJobPostRequest request) {
        if (request.getTitle() != null) {
            jobPost.setTitle(request.getTitle());
        }
        
        if (request.getDescription() != null) {
            jobPost.setDescription(request.getDescription());
        }
        
        if (request.getRequirements() != null) {
            jobPost.setRequirements(request.getRequirements());
        }
        
        if (request.getCompanyName() != null) {
            jobPost.setCompanyName(request.getCompanyName());
        }
        
        if (request.getLocation() != null) {
            if (jobPost.getAddress() == null) {
                Address address = new Address();
                address.setFormattedAddress(request.getLocation());
                jobPost.setAddress(address);
            } else {
                jobPost.getAddress().setFormattedAddress(request.getLocation());
            }
        }
        
        if (request.getWorkplaceType() != null) {
            jobPost.setWorkplaceType(request.getWorkplaceType());
        }
        
        if (request.getEmploymentType() != null) {
            jobPost.setEmploymentType(request.getEmploymentType());
        }
        
        if (request.getMinSalary() != null) {
            jobPost.setMinSalary(request.getMinSalary());
        }
        
        if (request.getMaxSalary() != null) {
            jobPost.setMaxSalary(request.getMaxSalary());
        }
        
        if (request.getCurrency() != null) {
            jobPost.setCurrency(request.getCurrency());
        }
        
        if (request.getActive() != null) {
            jobPost.setActive(request.getActive());
        }
        
        if (request.getVisibleUntil() != null) {
            jobPost.setVisibleUntil(request.getVisibleUntil());
        }
        
        return jobPost;
    }

    /**
     * Build a JobPostResponse from a JobPost entity
     */
    public JobPostResponse buildJobPostResponse(JobPost jobPost) {
        return JobPostResponse.builder()
                .id(jobPost.getId())
                .title(jobPost.getTitle())
                .description(jobPost.getDescription())
                .requirements(jobPost.getRequirements())
                .companyName(jobPost.getCompanyName())
                .location(jobPost.getAddress() != null ? jobPost.getAddress().getFormattedAddress() : null)
                .workplaceType(jobPost.getWorkplaceType())
                .employmentType(jobPost.getEmploymentType())
                .minSalary(jobPost.getMinSalary())
                .maxSalary(jobPost.getMaxSalary())
                .currency(jobPost.getCurrency())
                .salaryFormatted(jobPost.getSalaryRangeFormatted())
                .active(jobPost.isActive())
                .createdAt(jobPost.getCreatedAt())
                .updatedAt(jobPost.getUpdatedAt())
                .visibleUntil(jobPost.getVisibleUntil())
                .createdById(jobPost.getCreatedBy().getId())
                .createdByName(jobPost.getCreatedBy().getFullName())
                .skills(jobPost.getSkills().stream().map(Skill::getName).collect(Collectors.toSet()))
                .tags(jobPost.getTags().stream().map(Tag::getName).collect(Collectors.toSet()))
                .build();
    }

    /**
     * Build a JobPostSummaryResponse from a JobPost entity
     */
    public JobPostSummaryResponse buildJobPostSummaryResponse(JobPost jobPost) {
        return JobPostSummaryResponse.builder()
                .id(jobPost.getId())
                .title(jobPost.getTitle())
                .companyName(jobPost.getCompanyName())
                .location(jobPost.getAddress() != null ? jobPost.getAddress().getFormattedAddress() : null)
                .workplaceType(jobPost.getWorkplaceType())
                .employmentType(jobPost.getEmploymentType())
                .salaryFormatted(jobPost.getSalaryRangeFormatted())
                .active(jobPost.isActive())
                .createdAt(jobPost.getCreatedAt())
                .visibleUntil(jobPost.getVisibleUntil())
                .skills(jobPost.getSkills().stream()
                        .map(Skill::getName)
                        .collect(Collectors.toSet()))
                .tags(jobPost.getTags().stream()
                        .map(Tag::getName)
                        .collect(Collectors.toSet()))
                .build();
    }

    /**
     * Build a list of JobPostSummaryResponse from a list of JobPost entities
     */
    public List<JobPostSummaryResponse> buildJobPostSummaryResponses(List<JobPost> jobPosts) {
        return jobPosts.stream()
                .map(this::buildJobPostSummaryResponse)
                .collect(Collectors.toList());
    }
} 