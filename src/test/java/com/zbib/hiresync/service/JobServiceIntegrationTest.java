package com.zbib.hiresync.service;

import com.zbib.hiresync.dto.filter.JobFilter;
import com.zbib.hiresync.dto.request.CreateJobRequest;
import com.zbib.hiresync.dto.request.UpdateJobRequest;
import com.zbib.hiresync.dto.response.JobResponse;
import com.zbib.hiresync.dto.response.JobStatsResponse;
import com.zbib.hiresync.dto.response.JobSummaryResponse;
import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.entity.Skill;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.enums.ApplicationStatus;
import com.zbib.hiresync.enums.EmploymentType;
import com.zbib.hiresync.enums.WorkplaceType;
import com.zbib.hiresync.exception.job.JobNotFoundException;
import com.zbib.hiresync.exception.security.UnauthorizedException;
import com.zbib.hiresync.repository.ApplicationRepository;
import com.zbib.hiresync.repository.JobRepository;
import com.zbib.hiresync.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JobServiceIntegrationTest {

    @Autowired
    private JobService jobService;
    
    @Autowired
    private JobRepository jobRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ApplicationRepository applicationRepository;

    private User testUser;
    private Job testJob;
    private UUID jobId;
    private String username;

    @BeforeEach
    void setUp() {
        // Create a test user
        username = "testuser_" + UUID.randomUUID().toString().substring(0, 8);
        testUser = new User();
        testUser.setEmail(username);
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setPassword("password");
        testUser.setRole("RECRUITER");
        testUser.setEnabled(true);
        testUser.setLocked(false);
        testUser = userRepository.save(testUser);
        
        // Create a test job
        testJob = new Job();
        testJob.setTitle("Test Job");
        testJob.setDescription("This is a test job description");
        testJob.setRequirements("Requirements for test job");
        testJob.setCompanyName("Test Company");
        testJob.setMinSalary(BigDecimal.valueOf(50000));
        testJob.setMaxSalary(BigDecimal.valueOf(80000));
        testJob.setActive(true);
        testJob.setCreatedBy(testUser);
        testJob.setVisibleUntil(LocalDateTime.now().plusDays(30));
        testJob.setWorkplaceType(WorkplaceType.ONSITE);
        testJob.setEmploymentType(EmploymentType.FULL_TIME);
        testJob = jobRepository.save(testJob);
        jobId = testJob.getId();
    }

    @Test
    void getJobs_ShouldReturnUserJobs() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        JobFilter filter = new JobFilter();
        
        // Act
        Page<JobSummaryResponse> result = jobService.getJobs(filter, pageable, username);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.getTotalElements() > 0);
        assertEquals(testJob.getId(), result.getContent().get(0).getId());
    }
    
    @Test
    void getJobsFeed_ShouldReturnActiveJobs() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        JobFilter filter = new JobFilter();
        
        // Act
        Page<JobSummaryResponse> result = jobService.getJobsFeed(filter, pageable);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.getTotalElements() > 0);
    }
    
    @Test
    void getJobById_WhenJobExists_ShouldReturnJob() {
        // Act
        JobResponse result = jobService.getJobById(jobId, username);
        
        // Assert
        assertNotNull(result);
        assertEquals(jobId, result.getId());
        assertEquals(testJob.getTitle(), result.getTitle());
    }
    
    @Test
    void getJobById_WhenJobDoesNotExist_ShouldThrowException() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        
        // Act & Assert
        assertThrows(JobNotFoundException.class, () -> 
            jobService.getJobById(nonExistentId, username)
        );
    }
    
    @Test
    void createJob_ShouldCreateAndReturnJob() {
        // Arrange
        CreateJobRequest request = new CreateJobRequest();
        request.setTitle("New Integration Test Job");
        request.setDescription("This is a job created in an integration test");
        request.setRequirements("Job requirements");
        request.setCompanyName("Test Company");
        request.setLocation("Test Location");
        request.setMinSalary(BigDecimal.valueOf(60000));
        request.setMaxSalary(BigDecimal.valueOf(90000));
        request.setSkills(Set.of("Java", "Spring Boot"));
        request.setTags(Set.of("Remote", "Full-time"));
        request.setVisibleUntil(LocalDateTime.now().plusDays(30));
        request.setWorkplaceType(WorkplaceType.REMOTE);
        request.setEmploymentType(EmploymentType.CONTRACT);
        
        // Act
        JobResponse result = jobService.createJob(request, username);
        
        // Assert
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(request.getTitle(), result.getTitle());
        
        // Verify job was persisted
        assertTrue(jobRepository.findById(result.getId()).isPresent());
    }
    
    @Test
    void updateJob_WhenAuthorized_ShouldUpdateJob() {
        // Arrange
        String updatedTitle = "Updated Job Title";
        UpdateJobRequest request = new UpdateJobRequest();
        request.setTitle(updatedTitle);
        
        // Act
        JobResponse result = jobService.updateJob(jobId, request, username);
        
        // Assert
        assertNotNull(result);
        assertEquals(updatedTitle, result.getTitle());
        
        // Verify changes were persisted
        Job updatedJob = jobRepository.findById(jobId).orElseThrow();
        assertEquals(updatedTitle, updatedJob.getTitle());
    }
    
    @Test
    void updateJob_WhenUnauthorized_ShouldThrowException() {
        // Arrange
        String otherUsername = "otheruser_" + UUID.randomUUID().toString().substring(0, 8);
        User otherUser = new User();
        otherUser.setEmail(otherUsername);
        otherUser.setFirstName("Other");
        otherUser.setLastName("User");
        otherUser.setPassword("password");
        otherUser.setRole("RECRUITER");
        otherUser.setEnabled(true);
        otherUser.setLocked(false);
        userRepository.save(otherUser);
        
        UpdateJobRequest request = new UpdateJobRequest();
        request.setTitle("Should Not Update");
        
        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> 
            jobService.updateJob(jobId, request, otherUsername)
        );
    }
    
    @Test
    void deleteJob_WhenAuthorizedAndNoApplications_ShouldDeleteJob() {
        // Act
        jobService.deleteJob(jobId, username);
        
        // Assert
        assertFalse(jobRepository.findById(jobId).isPresent());
    }
    
    @Test
    void deleteJob_WhenJobHasApplications_ShouldThrowException() {
        // Arrange - Create an application for the job
        Application application = new Application();
        application.setJob(testJob);
        application.setApplicantName("Test Applicant");
        application.setApplicantEmail("test@example.com");
        application.setStatus(ApplicationStatus.SUBMITTED);
        applicationRepository.save(application);
        
        List<Application> applications = new ArrayList<>();
        applications.add(application);
        testJob.setApplications(applications);
        testJob.incrementApplicationCount();
        jobRepository.save(testJob);
        
        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> 
            jobService.deleteJob(jobId, username)
        );
        
        // Verify job still exists
        assertTrue(jobRepository.findById(jobId).isPresent());
    }
    
    @Test
    void toggleJobActiveStatus_ShouldToggleStatus() {
        // Arrange
        boolean initialStatus = testJob.isActive();
        
        // Act
        JobResponse result = jobService.toggleJobActiveStatus(jobId, username);
        
        // Assert
        assertNotNull(result);
        assertNotEquals(initialStatus, result.isActive());
        
        // Verify changes were persisted
        Job updatedJob = jobRepository.findById(jobId).orElseThrow();
        assertNotEquals(initialStatus, updatedJob.isActive());
    }
    
    @Test
    void extendJobVisibility_ShouldExtendVisibility() {
        // Arrange
        LocalDateTime initialVisibility = testJob.getVisibleUntil();
        int daysToExtend = 30;
        
        // Act
        JobResponse result = jobService.extendJobVisibility(jobId, username, daysToExtend);
        
        // Assert
        assertNotNull(result);
        
        // Verify changes were persisted
        Job updatedJob = jobRepository.findById(jobId).orElseThrow();
        assertTrue(updatedJob.getVisibleUntil().isAfter(initialVisibility));
    }
}