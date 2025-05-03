package com.zbib.hiresync.unit;

import com.zbib.hiresync.dto.builder.JobBuilder;
import com.zbib.hiresync.dto.filter.JobFilter;
import com.zbib.hiresync.dto.request.CreateJobRequest;
import com.zbib.hiresync.dto.request.UpdateJobRequest;
import com.zbib.hiresync.dto.response.JobResponse;
import com.zbib.hiresync.dto.response.JobSummaryResponse;
import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.exception.job.JobNotFoundException;
import com.zbib.hiresync.exception.security.UnauthorizedException;
import com.zbib.hiresync.repository.JobRepository;
import com.zbib.hiresync.service.JobService;
import com.zbib.hiresync.service.SkillService;
import com.zbib.hiresync.service.TagService;
import com.zbib.hiresync.service.UserService;
import com.zbib.hiresync.specification.JobSpecification;
import com.zbib.hiresync.validation.JobValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;
    
    @Mock
    private UserService userService;
    
    @Mock
    private JobBuilder jobBuilder;
    
    @Mock
    private SkillService skillService;
    
    @Mock
    private TagService tagService;
    
    @Mock
    private JobSpecification jobSpecification;
    
    @Mock
    private JobValidator jobValidator;
    
    @InjectMocks
    private JobService jobService;
    
    private User testUser;
    private Job testJob;
    private CreateJobRequest createJobRequest;
    private UpdateJobRequest updateJobRequest;
    private UUID jobId;
    private String username;
    
    @BeforeEach
    void setUp() {
        jobId = UUID.randomUUID();
        username = "testuser";
        
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail(username);
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setPassword("password");
        
        testJob = new Job();
        testJob.setId(jobId);
        testJob.setTitle("Test Job");
        testJob.setCreatedBy(testUser);
        testJob.setActive(true);
        
        createJobRequest = new CreateJobRequest();
        createJobRequest.setTitle("New Job");
        createJobRequest.setDescription("Job Description");
        
        updateJobRequest = new UpdateJobRequest();
        updateJobRequest.setTitle("Updated Job");
    }
    
    @Test
    void getJobs_ShouldReturnJobSummaries() {
        // Arrange
        JobFilter filter = new JobFilter();
        Pageable pageable = PageRequest.of(0, 10);
        List<Job> jobs = Collections.singletonList(testJob);
        Page<Job> jobPage = new PageImpl<>(jobs, pageable, 1);
        JobSummaryResponse jobSummary = new JobSummaryResponse();
        jobSummary.setId(jobId);
        
        when(userService.findByUsernameOrThrow(username)).thenReturn(testUser);
        Specification<Job> mockSpec = Mockito.mock(Specification.class);
        when(jobSpecification.buildSpecification(any(JobFilter.class))).thenReturn(mockSpec);
        when(jobRepository.findAll(Mockito.same(mockSpec), Mockito.same(pageable))).thenReturn(jobPage);
        when(jobBuilder.buildJobSummaryResponse(testJob)).thenReturn(jobSummary);
        
        // Act
        Page<JobSummaryResponse> result = jobService.getJobs(filter, pageable, username);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(jobId, result.getContent().get(0).getId());
        verify(userService).findByUsernameOrThrow(username);
        verify(jobRepository).findAll(Mockito.same(mockSpec), Mockito.same(pageable));
    }
    
    @Test
    void getJobsFeed_ShouldReturnActiveJobs() {
        // Arrange
        JobFilter filter = new JobFilter();
        Pageable pageable = PageRequest.of(0, 10);
        List<Job> jobs = Collections.singletonList(testJob);
        Page<Job> jobPage = new PageImpl<>(jobs, pageable, 1);
        JobSummaryResponse jobSummary = new JobSummaryResponse();
        jobSummary.setId(jobId);
        
        Specification<Job> mockSpec = Mockito.mock(Specification.class);
        when(jobSpecification.buildSpecification(any(JobFilter.class))).thenReturn(mockSpec);
        when(jobRepository.findAll(Mockito.same(mockSpec), Mockito.same(pageable))).thenReturn(jobPage);
        when(jobBuilder.buildJobSummaryResponse(testJob)).thenReturn(jobSummary);
        
        // Act
        Page<JobSummaryResponse> result = jobService.getJobsFeed(filter, pageable);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(jobId, result.getContent().get(0).getId());
        verify(jobRepository).findAll(Mockito.same(mockSpec), Mockito.same(pageable));
        verify(jobSpecification).buildSpecification(any(JobFilter.class));
    }
    
    @Test
    void getJobById_WhenAuthorized_ShouldReturnJob() {
        // Arrange
        JobResponse expectedResponse = new JobResponse();
        expectedResponse.setId(jobId);
        
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        when(jobBuilder.buildJobResponse(testJob)).thenReturn(expectedResponse);
        when(userService.findByUsernameOrThrow(username)).thenReturn(testUser);
        
        // Act
        JobResponse result = jobService.getJobById(jobId, username);
        
        // Assert
        assertNotNull(result);
        assertEquals(jobId, result.getId());
        verify(jobRepository).findById(jobId);
        verify(jobBuilder).buildJobResponse(testJob);
    }
    
    @Test
    void getJobById_WhenJobNotFound_ShouldThrowException() {
        // Arrange
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(JobNotFoundException.class, () -> 
            jobService.getJobById(jobId, username)
        );
        verify(jobRepository).findById(jobId);
    }
    
    @Test
    void createJob_ShouldCreateAndReturnJob() {
        // Arrange
        JobResponse expectedResponse = new JobResponse();
        expectedResponse.setId(jobId);
        
        when(userService.findByUsernameOrThrow(username)).thenReturn(testUser);
        when(jobBuilder.buildJob(eq(createJobRequest), eq(testUser), any(), any())).thenReturn(testJob);
        when(jobRepository.save(testJob)).thenReturn(testJob);
        when(jobBuilder.buildJobResponse(testJob)).thenReturn(expectedResponse);
        
        // Act
        JobResponse result = jobService.createJob(createJobRequest, username);
        
        // Assert
        assertNotNull(result);
        assertEquals(jobId, result.getId());
        verify(jobValidator).validateJobCompleteness(testJob);
        verify(jobRepository).save(testJob);
    }
    
    @Test
    void updateJob_WhenAuthorized_ShouldUpdateAndReturnJob() {
        // Arrange
        JobResponse expectedResponse = new JobResponse();
        expectedResponse.setId(jobId);
        
        when(userService.findByUsernameOrThrow(username)).thenReturn(testUser);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        when(jobRepository.save(testJob)).thenReturn(testJob);
        when(jobBuilder.buildJobResponse(testJob)).thenReturn(expectedResponse);
        
        // Act
        JobResponse result = jobService.updateJob(jobId, updateJobRequest, username);
        
        // Assert
        assertNotNull(result);
        assertEquals(jobId, result.getId());
        verify(jobBuilder).updateJob(eq(testJob), eq(updateJobRequest), any(), any());
        verify(jobValidator).validateJobCompleteness(testJob);
        verify(jobRepository).save(testJob);
    }
    
    @Test
    void updateJob_WhenUnauthorized_ShouldThrowException() {
        // Arrange
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        otherUser.setEmail("otheruser");
        otherUser.setFirstName("Other");
        otherUser.setLastName("User");
        otherUser.setPassword("password");
        
        testJob.setCreatedBy(otherUser);
        
        when(userService.findByUsernameOrThrow(username)).thenReturn(testUser);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        
        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> 
            jobService.updateJob(jobId, updateJobRequest, username)
        );
    }
    
    @Test
    void deleteJob_WhenAuthorizedAndNoApplications_ShouldDeleteJob() {
        // Arrange
        when(userService.findByUsernameOrThrow(username)).thenReturn(testUser);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        
        // Act
        jobService.deleteJob(jobId, username);
        
        // Assert
        verify(jobRepository).delete(testJob);
    }
    
    @Test
    void deleteJob_WhenJobHasApplications_ShouldThrowException() {
        // Arrange
        List<Application> applications = Collections.singletonList(new Application());
        testJob.setApplications(applications);
        testJob.incrementApplicationCount();
        
        when(userService.findByUsernameOrThrow(username)).thenReturn(testUser);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        
        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> 
            jobService.deleteJob(jobId, username)
        );
        verify(jobRepository, never()).delete(any(Job.class));
    }
    
    @Test
    void toggleJobActiveStatus_WhenAuthorized_ShouldToggleAndReturnJob() {
        // Arrange
        JobResponse expectedResponse = new JobResponse();
        expectedResponse.setId(jobId);
        boolean initialActiveStatus = testJob.isActive();
        
        when(userService.findByUsernameOrThrow(username)).thenReturn(testUser);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        when(jobRepository.save(testJob)).thenReturn(testJob);
        when(jobBuilder.buildJobResponse(testJob)).thenReturn(expectedResponse);
        
        // Act
        JobResponse result = jobService.toggleJobActiveStatus(jobId, username);
        
        // Assert
        assertNotNull(result);
        assertEquals(jobId, result.getId());
        assertNotEquals(initialActiveStatus, testJob.isActive());
        verify(jobRepository).save(testJob);
    }
    
    @Test
    void extendJobVisibility_WhenAuthorized_ShouldExtendAndReturnJob() {
        // Arrange
        JobResponse expectedResponse = new JobResponse();
        expectedResponse.setId(jobId);
        int days = 30;
        LocalDateTime originalVisibility = LocalDateTime.now();
        testJob.setVisibleUntil(originalVisibility);
        
        when(userService.findByUsernameOrThrow(username)).thenReturn(testUser);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        when(jobRepository.save(testJob)).thenReturn(testJob);
        when(jobBuilder.buildJobResponse(testJob)).thenReturn(expectedResponse);
        
        // Act
        JobResponse result = jobService.extendJobVisibility(jobId, username, days);
        
        // Assert
        assertNotNull(result);
        assertEquals(jobId, result.getId());
        verify(jobRepository).save(testJob);
    }
}