package com.zbib.hiresync.service;

import com.zbib.hiresync.dto.builder.ApplicationBuilder;
import com.zbib.hiresync.dto.response.ApplicationResponse;
import com.zbib.hiresync.dto.response.ApplicationSummaryResponse;
import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.JobPost;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.enums.ApplicationStatus;
import com.zbib.hiresync.exception.UnauthorizedException;
import com.zbib.hiresync.exception.application.ApplicationNotFoundException;
import com.zbib.hiresync.exception.jobpost.JobPostNotFoundException;
import com.zbib.hiresync.repository.ApplicationRepository;
import com.zbib.hiresync.repository.JobPostRepository;
import com.zbib.hiresync.specification.ApplicationSpecification;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private JobPostRepository jobPostRepository;

    @Mock
    private ApplicationBuilder applicationBuilder;

    @Mock
    private ApplicationSpecification applicationSpecification;

    @Mock
    private AuthService authService;

    @InjectMocks
    private ApplicationService applicationService;

    private UUID jobPostId;
    private JobPost jobPost;
    private User currentUser;
    private User jobPostCreator;
    private Application application1;
    private Application application2;
    private ApplicationSummaryResponse summaryResponse1;
    private ApplicationSummaryResponse summaryResponse2;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        jobPostId = UUID.randomUUID();
        UUID jobPostCreatorId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        
        // Create users
        jobPostCreator = new User();
        jobPostCreator.setId(jobPostCreatorId);
        
        currentUser = new User();
        currentUser.setId(currentUserId);
        
        // Create job post
        jobPost = new JobPost();
        jobPost.setId(jobPostId);
        jobPost.setCreatedBy(jobPostCreator);
        
        // Create applications
        application1 = new Application();
        application1.setId(UUID.randomUUID());
        application1.setJobPost(jobPost);
        application1.setApplicantName("John Applicant");
        application1.setApplicantEmail("john@example.com");
        application1.setStatus(ApplicationStatus.SUBMITTED);
        
        application2 = new Application();
        application2.setId(UUID.randomUUID());
        application2.setJobPost(jobPost);
        application2.setApplicantName("Jane Applicant");
        application2.setApplicantEmail("jane@example.com");
        application2.setStatus(ApplicationStatus.UNDER_REVIEW);
        
        // Create summary responses
        summaryResponse1 = ApplicationSummaryResponse.builder()
            .id(application1.getId())
            .applicantName("John Applicant")
            .applicantEmail("john@example.com")
            .status(ApplicationStatus.SUBMITTED)
            .build();
        
        summaryResponse2 = ApplicationSummaryResponse.builder()
            .id(application2.getId())
            .applicantName("Jane Applicant")
            .applicantEmail("jane@example.com")
            .status(ApplicationStatus.UNDER_REVIEW)
            .build();
        
        // Create pageable
        pageable = PageRequest.of(0, 10);
    }

    @Test
    void getApplicationsByJobPost_whenCurrentUserIsJobPostCreator_returnsApplicationsId() {
        // Given
        UUID jobPostId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        
        JobPost jobPost = new JobPost();
        jobPost.setId(jobPostId);
        
        User jobPostCreator = new User();
        jobPostCreator.setId(creatorId);
        jobPost.setCreatedBy(jobPostCreator);
        
        User currentUser = new User();
        currentUser.setId(creatorId); // Same ID as creator
        currentUser.setRole("EMPLOYER");
        
        List<Application> applications = Arrays.asList(
            createApplication(UUID.randomUUID()),
            createApplication(UUID.randomUUID())
        );
        Page<Application> applicationPage = new PageImpl<>(applications);
        
        // When
        when(jobPostRepository.findById(jobPostId)).thenReturn(Optional.of(jobPost));
        when(authService.getCurrentUser()).thenReturn(currentUser);
        when(applicationRepository.findByJobPost(eq(jobPost), any(Pageable.class))).thenReturn(applicationPage);
        when(applicationBuilder.buildApplicationResponse(any(Application.class))).thenAnswer(
            invocation -> {
                Application app = invocation.getArgument(0);
                return ApplicationResponse.builder()
                    .id(app.getId())
                    .build();
            }
        );
        
        List<ApplicationResponse> result = applicationService.getApplicationsByJobPostId(jobPostId);
        
        // Then
        assertEquals(2, result.size());
        verify(jobPostRepository).findById(jobPostId);
        verify(authService).getCurrentUser();
        verify(applicationRepository).findByJobPost(eq(jobPost), any(Pageable.class));
    }
    
    @Test
    void getApplicationsByJobPost_whenJobPostNotFound_throwsJobPostIdNotFoundException() {
        // Given
        UUID jobPostId = UUID.randomUUID();
        
        // When
        when(jobPostRepository.findById(jobPostId)).thenReturn(Optional.empty());
        when(authService.getCurrentUser()).thenReturn(currentUser);
        
        // Then
        assertThrows(JobPostNotFoundException.class, () -> 
            applicationService.getApplicationsByJobPostId(jobPostId));
        
        verify(jobPostRepository).findById(jobPostId);
        verify(applicationRepository, never()).findByJobPost(any(), any());
    }
    
    @Test
    void getApplicationsByJobPost_Id_whenUnauthorized_throwsUnauthorizedException() {
        // Given
        UUID jobPostId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID(); // Different from creator ID
        
        JobPost jobPost = new JobPost();
        jobPost.setId(jobPostId);
        
        User jobPostCreator = new User();
        jobPostCreator.setId(creatorId);
        jobPost.setCreatedBy(jobPostCreator);
        
        User currentUser = new User();
        currentUser.setId(currentUserId);
        currentUser.setRole("EMPLOYER");
        
        // When
        when(jobPostRepository.findById(jobPostId)).thenReturn(Optional.of(jobPost));
        when(authService.getCurrentUser()).thenReturn(currentUser);
        
        // Then
        assertThrows(UnauthorizedException.class, () -> 
            applicationService.getApplicationsByJobPostId(jobPostId));
        
        verify(jobPostRepository).findById(jobPostId);
        verify(authService).getCurrentUser();
        verify(applicationRepository, never()).findByJobPost(any(), any());
    }

    @Test
    void getApplicationById_whenApplicationNotFound_throwsApplicationNotFoundException() {
        // Given
        UUID applicationId = UUID.randomUUID();
        
        // When
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());
        
        // Then
        assertThrows(ApplicationNotFoundException.class, () -> 
            applicationService.getApplicationById(applicationId));
            
        verify(applicationRepository).findById(applicationId);
    }

    // Helper method to create an application with a specific ID
    private Application createApplication(UUID id) {
        Application application = new Application();
        application.setId(id);
        application.setApplicantName("Test Applicant");
        application.setApplicantEmail("test@example.com");
        application.setStatus(ApplicationStatus.SUBMITTED);
        return application;
    }
} 