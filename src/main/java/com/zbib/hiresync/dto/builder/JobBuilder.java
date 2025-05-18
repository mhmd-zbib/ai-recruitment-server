package com.zbib.hiresync.dto.builder;

import com.zbib.hiresync.dto.request.CreateJobRequest;
import com.zbib.hiresync.dto.request.UpdateJobRequest;
import com.zbib.hiresync.dto.response.JobResponse;

import com.zbib.hiresync.dto.response.JobListResponse;
import com.zbib.hiresync.entity.Address;
import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.entity.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class JobBuilder {

    public Job buildJob(CreateJobRequest request, User creator) {
        LocalDateTime now = LocalDateTime.now();

        Job job = Job.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .requirements(request.getRequirements())
                .companyName(request.getCompanyName())
                .workplaceType(request.getWorkplaceType())
                .employmentType(request.getEmploymentType())
                .minSalary(request.getMinSalary())
                .maxSalary(request.getMaxSalary())
                .currency(request.getCurrency())
                .user(creator)
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        if (request.getLocation() != null && !request.getLocation().isBlank()) {
            Address address = Address.builder()
                    .formattedAddress(request.getLocation())
                    .build();
            job.setAddress(address);
        }

        return job;
    }

    public void updateJob(Job job, UpdateJobRequest request) {
        if (request.getTitle() != null) {
            job.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            job.setDescription(request.getDescription());
        }

        if (request.getRequirements() != null) {
            job.setRequirements(request.getRequirements());
        }

        if (request.getCompanyName() != null) {
            job.setCompanyName(request.getCompanyName());
        }

        if (request.getWorkplaceType() != null) {
            job.setWorkplaceType(request.getWorkplaceType());
        }

        if (request.getEmploymentType() != null) {
            job.setEmploymentType(request.getEmploymentType());
        }


        if (request.getMinSalary() != null) {
            job.setMinSalary(request.getMinSalary());
        }

        if (request.getMaxSalary() != null) {
            job.setMaxSalary(request.getMaxSalary());
        }

        if (request.getCurrency() != null) {
            job.setCurrency(request.getCurrency());
        }

        updateAddress(job, request.getLocation());
    }

    private void updateAddress(Job job, String location) {
        if (location == null) {
            return;
        }

        if (job.getAddress() == null) {
            Address address = Address.builder()
                    .formattedAddress(location)
                    .build();
            job.setAddress(address);
        } else {
            job.getAddress().setFormattedAddress(location);
        }
    }

    public JobResponse buildJobResponse(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .requirements(job.getRequirements())
                .companyName(job.getCompanyName())
                .location(job.getAddress() != null ? job.getAddress().getFormattedAddress() : null)
                .workplaceType(job.getWorkplaceType())
                .employmentType(job.getEmploymentType())
                .minSalary(job.getMinSalary())
                .maxSalary(job.getMaxSalary())
                .currency(job.getCurrency())
                .active(job.isActive())
                .applicationCount(job.getApplicationCount())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    public JobListResponse buildJobListResponse(Job job) {
        return JobListResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .workplaceType(job.getWorkplaceType())
                .employmentType(job.getEmploymentType())
                .active(job.isActive())
                .applicationCount(job.getApplicationCount())
                .createdAt(job.getCreatedAt())
                .build();
    }
}