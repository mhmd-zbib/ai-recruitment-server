package com.zbib.hiresync.service;

import com.zbib.hiresync.dto.builder.JobBuilder;
import com.zbib.hiresync.dto.filter.JobFilter;
import com.zbib.hiresync.dto.request.CreateJobRequest;
import com.zbib.hiresync.dto.request.UpdateJobRequest;
import com.zbib.hiresync.dto.response.JobResponse;
import com.zbib.hiresync.dto.response.JobStatsResponse;
import com.zbib.hiresync.dto.response.JobSummaryResponse;
import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.entity.Skill;
import com.zbib.hiresync.entity.Tag;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.enums.ApplicationStatus;
import com.zbib.hiresync.exception.ResourceNotFoundException;
import com.zbib.hiresync.exception.auth.UserNotFoundException;
import com.zbib.hiresync.exception.job.InvalidJobStateException;
import com.zbib.hiresync.exception.security.UnauthorizedException;
import com.zbib.hiresync.logging.LoggableService;
import com.zbib.hiresync.repository.ApplicationRepository;
import com.zbib.hiresync.repository.JobRepository;
import com.zbib.hiresync.repository.UserRepository;
import com.zbib.hiresync.specification.JobSpecification;
import com.zbib.hiresync.validator.JobValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @Transactional(readOnly = true)
    @LoggableService(message = "Retrieved jobs with filtering")
    public Page<JobSummaryResponse> getJobs(JobFilter filter, Pageable pageable, String username) {
        if (username == null) {
            filter.setActive(true);
            filter.setVisibleAfter(LocalDateTime.now());
        } else {
            User currentUser = userService.findByUsernameOrThrow(username);
            filter.setCreatedById(currentUser.getId());
        }
        
        Page<Job> jobs = jobRepository.findAll(jobSpecification.buildSpecification(filter), pageable);
        return jobs.map(jobBuilder::buildJobSummaryResponse);
    }
    
    @Transactional(readOnly = true)
    @LoggableService(message = "Retrieved job by ID")
    public JobResponse getJobById(UUID id, String username) {
        Job job = findJobByIdOrThrow(id);
        
        if (username == null && (!job.isActive() || job.isExpired())) {
            throw new ResourceNotFoundException("Job not found with ID: " + id);
        }
        
        if (username != null) {
            User user = userService.findByUsernameOrThrow(username);
            if (!job.isActive() && !job.isOwnedBy(user)) {
                throw new ResourceNotFoundException("Job not found with ID: " + id);
            }
        }

        return jobBuilder.buildJobResponse(job);
    }
    
    @Transactional
    @LoggableService(message = "Created job")
    public JobResponse createJob(CreateJobRequest request, String username) {
        User currentUser = userService.findByUsernameOrThrow(username);
        
        Job job = jobBuilder.buildJob(request, currentUser, new HashSet<>(), new HashSet<>());
        
        jobValidator.validateJobCompleteness(job);
        
        if (request.getSkills() != null && !request.getSkills().isEmpty()) {
            skillService.addSkillsToJob(job, request.getSkills());
        }
        
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            tagService.addTagsToJob(job, request.getTags());
        }
        
        Job savedJob = jobRepository.save(job);
        return jobBuilder.buildJobResponse(savedJob);
    }
    
    @Transactional
    @LoggableService(message = "Updated job")
    public JobResponse updateJob(UUID id, UpdateJobRequest request, String username) {
        User currentUser = userService.findByUsernameOrThrow(username);
        Job job = findJobByIdOrThrow(id);
        
        if (!job.isOwnedBy(currentUser)) {
            throw UnauthorizedException.modifyJobPost();
        }
        
        jobBuilder.updateJob(job, request, new HashSet<>(), new HashSet<>());
        
        if (request.getSkills() != null) {
            job.getSkills().clear();
            skillService.addSkillsToJob(job, request.getSkills());
        }
        
        if (request.getTags() != null) {
            job.getTags().clear();
            tagService.addTagsToJob(job, request.getTags());
        }
        
        jobValidator.validateJobCompleteness(job);
        Job updatedJob = jobRepository.save(job);
        return jobBuilder.buildJobResponse(updatedJob);
    }
    
    @Transactional
    @LoggableService(message = "Deleted job")
    public void deleteJob(UUID id, String username) {
        User currentUser = userService.findByUsernameOrThrow(username);
        Job job = findJobByIdOrThrow(id);
        
        if (!job.isOwnedBy(currentUser)) {
            throw UnauthorizedException.deleteJobPost();
        }
        
        if (job.hasApplications()) {
            throw UnauthorizedException.deleteJobPostWithApplications();
        }
        
        jobRepository.delete(job);
    }
    
    @Transactional(readOnly = true)
    @LoggableService(message = "Retrieved job statistics")
    public JobStatsResponse getJobStats(UUID id, String username) {
        User currentUser = userService.findByUsernameOrThrow(username);
        Job job = findJobByIdOrThrow(id);
    
        if (!job.isOwnedBy(currentUser)) {
            throw new UnauthorizedException("You are not authorized to view this job's statistics");
        }
        
        List<Application> applications = job.getApplications();
        
        Map<ApplicationStatus, Long> applicationsByStatus = applications.stream()
                .collect(Collectors.groupingBy(Application::getStatus, Collectors.counting()));
        
        Map<String, Long> applicantsByTopSkills = applications.stream()
                .flatMap(app -> app.getSkillNames().stream())
                .collect(Collectors.groupingBy(skill -> skill, Collectors.counting()));
        
        return jobBuilder.buildJobStatsResponse(job, applicationsByStatus, applicantsByTopSkills);
    }
    
    @Transactional
    @LoggableService(message = "Toggled job active status")
    public JobResponse toggleJobActiveStatus(UUID id, String username) {
        User currentUser = userService.findByUsernameOrThrow(username);
        Job job = findJobByIdOrThrow(id);
        
        if (!job.isOwnedBy(currentUser)) {
            throw UnauthorizedException.modifyJobPost();
        }
        
        if (job.isActive()) {
            job.deactivate();
        } else {
            jobValidator.validateCanBeActivated(job);
            job.activate();
        }
        
        job.updateTimestamp();
        Job savedJob = jobRepository.save(job);
        return jobBuilder.buildJobResponse(savedJob);
    }
    
    @Transactional
    @LoggableService(message = "Extended job visibility")
    public JobResponse extendJobVisibility(UUID id, String username, int days) {
        User currentUser = userService.findByUsernameOrThrow(username);
        Job job = findJobByIdOrThrow(id);
        
        if (!job.isOwnedBy(currentUser)) {
            throw UnauthorizedException.modifyJobPost();
        }
        
        job.extendVisibilityBy(days);
        job.updateTimestamp();
        
        Job savedJob = jobRepository.save(job);
        return jobBuilder.buildJobResponse(savedJob);
    }
    
    private Job findJobByIdOrThrow(UUID id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.jobPost(id));
    }
}
