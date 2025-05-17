package com.zbib.hiresync.service;

import com.zbib.hiresync.dto.builder.JobBuilder;
import com.zbib.hiresync.dto.filter.JobFilter;
import com.zbib.hiresync.dto.request.CreateJobRequest;
import com.zbib.hiresync.dto.request.UpdateJobRequest;
import com.zbib.hiresync.dto.response.JobListResponse;
import com.zbib.hiresync.dto.response.JobResponse;
import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.entity.Skill;
import com.zbib.hiresync.entity.Tag;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.exception.AuthException;
import com.zbib.hiresync.exception.JobException;
import com.zbib.hiresync.repository.JobRepository;
import com.zbib.hiresync.specification.JobSpecification;
import com.zbib.hiresync.validation.JobValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final UserService userService;
    private final JobBuilder jobBuilder;
    private final SkillService skillService;
    private final TagService tagService;
    private final JobSpecification jobSpecification;
    private final JobValidator jobValidator;

    @Transactional
    public JobResponse createJob(CreateJobRequest request, String username) {
        User user = userService.findByUsernameOrThrow(username);
        
        Set<Skill> skills = skillService.getOrCreateSkills(request.getSkills());
        Set<Tag> tags = tagService.getOrCreateTags(request.getTags());
        
        Job job = jobBuilder.buildJob(request, user, skills, tags);
        jobValidator.validateJobCompleteness(job);
        
        Job savedJob = jobRepository.save(job);
        
        return jobBuilder.buildJobResponse(savedJob);
    }
    
    public Page<JobListResponse> getJobs(JobFilter filter, Pageable pageable, String username) {
        User user = userService.findByUsernameOrThrow(username);
        
        filter.setCreatedById(user.getId());
        
        Specification<Job> spec = jobSpecification.buildSpecification(filter);
        
        Page<Job> jobsPage = jobRepository.findAll(spec, pageable);
        
        return jobsPage.map(jobBuilder::buildJobListResponse);
    }
    
    public Page<JobListResponse> getJobsFeed(JobFilter filter, Pageable pageable) {
        filter.setActive(true);
        
        Specification<Job> spec = jobSpecification.buildSpecification(filter);
        
        Page<Job> jobsPage = jobRepository.findAll(spec, pageable);
        
        return jobsPage.map(jobBuilder::buildJobListResponse);
    }
    
    @Transactional
    public JobResponse updateJob(UUID jobId, UpdateJobRequest request, String username) {
        User user = userService.findByUsernameOrThrow(username);
        Job job = findJobByIdOrThrow(jobId);
        
        if (!job.isOwnedBy(user)) {
            throw AuthException.accessDenied("job", jobId, username);
        }
        
        Set<Skill> skills = skillService.getOrCreateSkills(request.getSkills());
        Set<Tag> tags = tagService.getOrCreateTags(request.getTags());
        
        jobBuilder.updateJob(job, request, skills, tags);
        jobValidator.validateJobCompleteness(job);
        
        Job updatedJob = jobRepository.save(job);
        
        return jobBuilder.buildJobResponse(updatedJob);
    }
    
    @Transactional
    public void deleteJob(UUID jobId, String username) {
        User user = userService.findByUsernameOrThrow(username);
        Job job = findJobByIdOrThrow(jobId);
        
        if (!job.isOwnedBy(user)) {
            throw AuthException.accessDenied("job", jobId, username);
        }
        
        if (job.hasApplications()) {
            throw JobException.hasApplications(jobId);
        }
        
        jobRepository.delete(job);
    }
    
    private Job findJobByIdOrThrow(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> JobException.notFound(jobId));
    }
}