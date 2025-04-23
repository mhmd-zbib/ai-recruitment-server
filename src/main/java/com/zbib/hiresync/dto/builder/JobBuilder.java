package com.zbib.hiresync.dto.builder;

import com.zbib.hiresync.dto.request.CreateJobRequest;
import com.zbib.hiresync.dto.request.UpdateJobRequest;
import com.zbib.hiresync.dto.response.JobResponse;
import com.zbib.hiresync.dto.response.JobStatsResponse;
import com.zbib.hiresync.dto.response.JobSummaryResponse;
import com.zbib.hiresync.entity.Address;
import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.entity.Skill;
import com.zbib.hiresync.entity.Tag;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.enums.ApplicationStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JobBuilder {

    public Job buildJob(CreateJobRequest request, User creator, Set<Skill> skills, Set<Tag> tags) {
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
                .createdBy(creator)
                .active(false)
                .visibleUntil(request.getVisibleUntil())
                .createdAt(now)
                .updatedAt(now)
                .skills(skills)
                .tags(tags)
                .build();
        
        if (request.getLocation() != null && !request.getLocation().isBlank()) {
            Address address = Address.builder()
                    .formattedAddress(request.getLocation())
                    .build();
            job.setAddress(address);
        }
        
        return job;
    }
    
    public void updateJob(Job job, UpdateJobRequest request, Set<Skill> skills, Set<Tag> tags) {
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
        
        if (request.getVisibleUntil() != null) {
            job.setVisibleUntil(request.getVisibleUntil());
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
        
        if (!skills.isEmpty()) {
            job.setSkills(skills);
        }
        
        if (!tags.isEmpty()) {
            job.setTags(tags);
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
                .visibleUntil(job.getVisibleUntil())
                .skills(job.getSkills().stream().map(Skill::getName).collect(Collectors.toSet()))
                .tags(job.getTags().stream().map(Tag::getName).collect(Collectors.toSet()))
                .applicationCount(job.getApplications() != null ? job.getApplications().size() : 0)
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
    
    public JobSummaryResponse buildJobSummaryResponse(Job job) {
        return JobSummaryResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .companyName(job.getCompanyName())
                .location(job.getAddress() != null ? job.getAddress().getFormattedAddress() : null)
                .workplaceType(job.getWorkplaceType())
                .employmentType(job.getEmploymentType())
                .minSalary(job.getMinSalary())
                .maxSalary(job.getMaxSalary())
                .currency(job.getCurrency())
                .active(job.isActive())
                .visibleUntil(job.getVisibleUntil())
                .skills(job.getSkills().stream().map(Skill::getName).collect(Collectors.toSet()))
                .tags(job.getTags().stream().map(Tag::getName).collect(Collectors.toSet()))
                .applicationCount(job.getApplications() != null ? job.getApplications().size() : 0)
                .createdAt(job.getCreatedAt())
                .build();
    }
    
    public JobStatsResponse buildJobStatsResponse(Job job, Map<ApplicationStatus, Long> applicationsByStatus, Map<String, Long> applicantsByTopSkills) {
        long totalViews = job.getApplications() != null ? job.getApplications().size() * 5 : 0;
        
        return JobStatsResponse.builder()
                .totalViews(totalViews)
                .totalApplications(job.getApplications() != null ? job.getApplications().size() : 0)
                .applicationsByStatus(applicationsByStatus)
                .applicantsByTopSkills(applicantsByTopSkills)
                .build();
    }
}