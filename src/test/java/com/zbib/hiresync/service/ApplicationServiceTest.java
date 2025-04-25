package com.zbib.hiresync.service;

import com.zbib.hiresync.dto.JobCountDTO;
import com.zbib.hiresync.dto.StatusCountDTO;
import com.zbib.hiresync.dto.builder.ApplicationBuilder;
import com.zbib.hiresync.dto.filter.ApplicationFilter;
import com.zbib.hiresync.dto.request.CreateApplicationRequest;
import com.zbib.hiresync.dto.request.UpdateApplicationStatusRequest;
import com.zbib.hiresync.dto.response.ApplicationResponse;
import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.enums.ApplicationStatus;
import com.zbib.hiresync.exception.ResourceNotFoundException;
import com.zbib.hiresync.exception.application.ApplicationAlreadyExistException;
import com.zbib.hiresync.exception.security.UnauthorizedException;
import com.zbib.hiresync.repository.ApplicationRepository;
import com.zbib.hiresync.repository.JobRepository;
import com.zbib.hiresync.specification.ApplicationSpecification;
import com.zbib.hiresync.validation.ApplicationValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private UserService userService;

    @Mock
    private ApplicationBuilder applicationBuilder;

    @Mock
    private ApplicationSpecification applicationSpecification;

    @Mock
    private ApplicationValidator applicationValidator;

    @InjectMocks
    private ApplicationService applicationService;

    private UUID jobId;
    private UUID applicationId;
    private String username;
    private User user;
    private Job job;
    private Application application;
    private CreateApplicationRequest createRequest;
    private ApplicationResponse applicationResponse;
    private ApplicationFilter filter;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        jobId = UUID.randomUUID();
        applicationId = UUID.randomUUID();
        username = "test@example.com";
        
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(username);
        
        job = new Job();
        job.setId(jobId);
        job.setActive(true);
        job.setCreatedBy(user);
        
        application = new Application();
        application.setId(applicationId);
        application.setJob(job);
        application.setApplicantEmail("applicant@example.com");
        application.setStatus(ApplicationStatus.SUBMITTED);
        
        createRequest = new CreateApplicationRequest();
        createRequest.setJobId(jobId);
        createRequest.setApplicantEmail("applicant@example.com");
        
        applicationResponse = new ApplicationResponse();
        applicationResponse.setId(applicationId);
        
        filter = new ApplicationFilter();
        pageable = PageRequest.of(0, 10);
    }

    @Test
    void createApplication_Success() {
        // Arrange
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(applicationRepository.existsByJobAndApplicantEmail(job, createRequest.getApplicantEmail())).thenReturn(false);
        when(applicationBuilder.buildApplication(createRequest, job)).thenReturn(application);
        when(applicationRepository.save(application)).thenReturn(application);
        when(applicationBuilder.buildApplicationResponse(application)).thenReturn(applicationResponse);

        // Act
        ApplicationResponse result = applicationService.createApplication(createRequest);

        // Assert
        assertNotNull(result);
        assertEquals(applicationResponse, result);
        verify(jobRepository).findById(jobId);
        verify(applicationRepository).existsByJobAndApplicantEmail(job, createRequest.getApplicantEmail());
        verify(applicationValidator).validateNewApplication(application);
        verify(jobRepository).save(job);
        verify(applicationRepository).save(application);
    }

    @Test
    void createApplication_JobNotFound() {
        // Arrange
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            applicationService.createApplication(createRequest);
        });
    }

    @Test
    void createApplication_JobNotActive() {
        // Arrange
        job.setActive(false);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            applicationService.createApplication(createRequest);
        });
    }

    @Test
    void createApplication_ApplicationAlreadyExists() {
        // Arrange
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(applicationRepository.existsByJobAndApplicantEmail(job, createRequest.getApplicantEmail())).thenReturn(true);

        // Act & Assert
        assertThrows(ApplicationAlreadyExistException.class, () -> {
            applicationService.createApplication(createRequest);
        });
    }

    @Test
    void updateApplicationStatus_Success() {
        // Arrange
        UpdateApplicationStatusRequest request = new UpdateApplicationStatusRequest();
        request.setStatus(ApplicationStatus.UNDER_REVIEW);
        request.setNotes("Under review notes");

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(userService.findByUsernameOrThrow(username)).thenReturn(user);
        when(applicationRepository.save(application)).thenReturn(application);
        when(applicationBuilder.buildApplicationResponse(application)).thenReturn(applicationResponse);

        // Act
        ApplicationResponse result = applicationService.updateApplicationStatus(applicationId, request, username);

        // Assert
        assertNotNull(result);
        assertEquals(applicationResponse, result);
        verify(applicationRepository).findById(applicationId);
        verify(userService).findByUsernameOrThrow(username);
        verify(applicationValidator).validateStatusTransition(application, request.getStatus());
        verify(applicationRepository).save(application);
    }

    @Test
    void updateApplicationStatus_ApplicationNotFound() {
        // Arrange
        UpdateApplicationStatusRequest request = new UpdateApplicationStatusRequest();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            applicationService.updateApplicationStatus(applicationId, request, username);
        });
    }

    @Test
    void updateApplicationStatus_UnauthorizedUser() {
        // Arrange
        UpdateApplicationStatusRequest request = new UpdateApplicationStatusRequest();
        User differentUser = new User();
        differentUser.setId(UUID.randomUUID());
        
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(userService.findByUsernameOrThrow(username)).thenReturn(differentUser);

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> {
            applicationService.updateApplicationStatus(applicationId, request, username);
        });
    }

    @Test
    void deleteApplication_Success() {
        // Arrange
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(userService.findByUsernameOrThrow(username)).thenReturn(user);

        // Act
        applicationService.deleteApplication(applicationId, username);

        // Assert
        verify(applicationRepository).findById(applicationId);
        verify(userService).findByUsernameOrThrow(username);
        verify(applicationRepository).delete(application);
    }

    @Test
    void deleteApplication_ApplicationNotFound() {
        // Arrange
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            applicationService.deleteApplication(applicationId, username);
        });
    }

    @Test
    void deleteApplication_UnauthorizedUser() {
        // Arrange
        User differentUser = new User();
        differentUser.setId(UUID.randomUUID());
        
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(userService.findByUsernameOrThrow(username)).thenReturn(differentUser);

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> {
            applicationService.deleteApplication(applicationId, username);
        });
    }

    @Test
    void getApplicationById_Success() {
        // Arrange
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(userService.findByUsernameOrThrow(username)).thenReturn(user);
        when(applicationBuilder.buildApplicationResponse(application)).thenReturn(applicationResponse);

        // Act
        ApplicationResponse result = applicationService.getApplicationById(applicationId, username);

        // Assert
        assertNotNull(result);
        assertEquals(applicationResponse, result);
        verify(applicationRepository).findById(applicationId);
        verify(userService).findByUsernameOrThrow(username);
    }

    @Test
    void getApplicationById_ApplicationNotFound() {
        // Arrange
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            applicationService.getApplicationById(applicationId, username);
        });
    }

    @Test
    void getAllApplications_Success() {
        // Arrange
        List<Application> applications = Collections.singletonList(application);
        Page<Application> applicationPage = new PageImpl<>(applications);
        
        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        when(applicationSpecification.buildSpecificationForUserJobs(any(), any())).thenReturn(null);
        when(applicationRepository.findAll(nullable(Specification.class), any(Pageable.class))).thenReturn(applicationPage);
        when(applicationBuilder.buildApplicationResponse(any())).thenReturn(applicationResponse);

        // Act
        Page<ApplicationResponse> result = applicationService.getAllApplications(filter, pageable, username);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(userService).findByUsernameOrThrow(anyString());
        verify(applicationSpecification).buildSpecificationForUserJobs(any(), any());
        verify(applicationRepository).findAll(nullable(Specification.class), any(Pageable.class));
    }

    @Test
    void getApplicationsByStatus_Success() {
        // Arrange
        List<Application> applications = Collections.singletonList(application);
        Page<Application> applicationPage = new PageImpl<>(applications);
        ApplicationStatus status = ApplicationStatus.SUBMITTED;
        
        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        when(applicationSpecification.buildSpecificationForUserJobs(any(), any())).thenReturn(null);
        when(applicationRepository.findAll(nullable(Specification.class), any(Pageable.class))).thenReturn(applicationPage);
        when(applicationBuilder.buildApplicationResponse(any())).thenReturn(applicationResponse);

        // Act
        Page<ApplicationResponse> result = applicationService.getApplicationsByStatus(status, pageable, username);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(userService).findByUsernameOrThrow(anyString());
        verify(applicationSpecification).buildSpecificationForUserJobs(any(), any());
        verify(applicationRepository).findAll(nullable(Specification.class), any(Pageable.class));
    }

    @Test
    void getRecentApplications_Success() {
        // Arrange
        List<Application> applications = Collections.singletonList(application);
        
        when(userService.findByUsernameOrThrow(username)).thenReturn(user);
        when(applicationRepository.findRecentApplicationsForCreatedBy(eq(user), anyInt())).thenReturn(applications);
        when(applicationBuilder.buildApplicationResponse(application)).thenReturn(applicationResponse);

        // Act
        List<ApplicationResponse> result = applicationService.getRecentApplications(username);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userService).findByUsernameOrThrow(username);
        verify(applicationRepository).findRecentApplicationsForCreatedBy(eq(user), anyInt());
    }

    @Test
    void getApplicationStats_Success() {
        // Arrange
        long totalApplications = 10L;
        
        StatusCountDTO statusCountDTO = new StatusCountDTO();
        statusCountDTO.setStatus(ApplicationStatus.SUBMITTED);
        statusCountDTO.setCount(5L);
        List<StatusCountDTO> statusCounts = Collections.singletonList(statusCountDTO);
        
        JobCountDTO jobCountDTO = new JobCountDTO();
        jobCountDTO.setJobId(jobId);
        jobCountDTO.setCount(5L);
        List<JobCountDTO> jobCounts = Collections.singletonList(jobCountDTO);
        
        when(userService.findByUsernameOrThrow(username)).thenReturn(user);
        when(applicationRepository.countByJobPostCreatedBy(user)).thenReturn(totalApplications);
        when(applicationRepository.countApplicationsByStatusForUser(user)).thenReturn(statusCounts);
        when(applicationRepository.countApplicationsByJobPostForUser(user)).thenReturn(jobCounts);

        // Act
        var result = applicationService.getApplicationStats(username);

        // Assert
        assertNotNull(result);
        assertEquals(totalApplications, result.getTotalApplications());
        assertEquals(1, result.getApplicationsByStatus().size());
        assertEquals(1, result.getApplicationsByJob().size());
        assertEquals(5L, result.getApplicationsByStatus().get(ApplicationStatus.SUBMITTED));
        assertEquals(5L, result.getApplicationsByJob().get(jobId));
        
        verify(userService).findByUsernameOrThrow(username);
        verify(applicationRepository).countByJobPostCreatedBy(user);
        verify(applicationRepository).countApplicationsByStatusForUser(user);
        verify(applicationRepository).countApplicationsByJobPostForUser(user);
    }
} 