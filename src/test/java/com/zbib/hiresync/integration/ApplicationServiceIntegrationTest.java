package com.zbib.hiresync.integration;

import com.zbib.hiresync.dto.filter.ApplicationFilter;
import com.zbib.hiresync.dto.request.CreateApplicationRequest;
import com.zbib.hiresync.dto.request.UpdateApplicationStatusRequest;
import com.zbib.hiresync.dto.response.ApplicationResponse;
import com.zbib.hiresync.dto.response.ApplicationStatsResponse;
import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.enums.ApplicationStatus;
import com.zbib.hiresync.enums.EmploymentType;
import com.zbib.hiresync.enums.WorkplaceType;
import com.zbib.hiresync.repository.ApplicationRepository;
import com.zbib.hiresync.repository.JobRepository;
import com.zbib.hiresync.repository.UserRepository;
import com.zbib.hiresync.service.ApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ApplicationServiceIntegrationTest {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Job testJob;
    private String email;

    @BeforeEach
    void setUp() {
        // Create test user
        email = "test.user@example.com";
        testUser = createTestUser(email);
        userRepository.save(testUser);

        // Create test job
        testJob = createTestJob(testUser);
        jobRepository.save(testJob);
    }

    @Test
    void createAndRetrieveApplication() {
        // Create application
        CreateApplicationRequest request = new CreateApplicationRequest();
        request.setJobId(testJob.getId());
        request.setApplicantEmail("applicant@example.com");
        request.setApplicantName("Test Applicant");
        request.setCoverLetter("I'm very interested in this position and would like to apply. I have extensive experience in the required skills and believe I would be a great fit for your team. Please consider my application.");
        request.setResumeUrl("https://resume.example.com/123");

        ApplicationResponse createdApplication = applicationService.createApplication(request);

        // Verify created application
        assertNotNull(createdApplication.getId());
        assertEquals(request.getApplicantEmail(), createdApplication.getApplicantEmail());
        assertEquals(request.getApplicantName(), createdApplication.getApplicantName());
        assertEquals(ApplicationStatus.SUBMITTED, createdApplication.getStatus());

        // Retrieve and verify by ID
        ApplicationResponse retrievedApplication = applicationService.getApplicationById(
                createdApplication.getId(), email);

        assertEquals(createdApplication.getId(), retrievedApplication.getId());
        assertEquals(request.getApplicantEmail(), retrievedApplication.getApplicantEmail());
    }

    @Test
    void updateApplicationStatus() {
        // Create application
        Application application = createTestApplication(testJob);
        applicationRepository.save(application);

        // Update status
        UpdateApplicationStatusRequest request = new UpdateApplicationStatusRequest();
        request.setStatus(ApplicationStatus.UNDER_REVIEW);
        request.setNotes("This candidate looks promising");

        ApplicationResponse updatedApplication = applicationService.updateApplicationStatus(
                application.getId(), request, email);

        // Verify update
        assertEquals(ApplicationStatus.UNDER_REVIEW, updatedApplication.getStatus());
        assertEquals(request.getNotes(), updatedApplication.getNotes());

        // Verify in database
        Application dbApplication = applicationRepository.findById(application.getId()).orElseThrow();
        assertEquals(ApplicationStatus.UNDER_REVIEW, dbApplication.getStatus());
        assertEquals(request.getNotes(), dbApplication.getNotes());
    }

    @Test
    void getAllApplicationsForUser() {
        // Create multiple applications
        Application app1 = createTestApplication(testJob);
        Application app2 = createTestApplication(testJob);
        applicationRepository.save(app1);
        applicationRepository.save(app2);

        // Get all applications
        ApplicationFilter filter = new ApplicationFilter();
        Page<ApplicationResponse> applications = applicationService.getAllApplications(
                filter, PageRequest.of(0, 10), email);

        // Verify applications
        assertEquals(2, applications.getTotalElements());
    }

    @Test
    void getApplicationsByStatus() {
        // Create applications with different statuses
        Application app1 = createTestApplication(testJob);
        app1.setStatus(ApplicationStatus.SUBMITTED);

        Application app2 = createTestApplication(testJob);
        app2.setStatus(ApplicationStatus.UNDER_REVIEW);

        applicationRepository.save(app1);
        applicationRepository.save(app2);

        // Get applications by status
        Page<ApplicationResponse> submittedApplications = applicationService.getApplicationsByStatus(
                ApplicationStatus.SUBMITTED, PageRequest.of(0, 10), email);

        // Verify applications
        assertEquals(1, submittedApplications.getTotalElements());
        assertEquals(ApplicationStatus.SUBMITTED, submittedApplications.getContent().get(0).getStatus());
    }

    @Test
    void getApplicationStats() {
        // Create applications with different statuses
        Application app1 = createTestApplication(testJob);
        app1.setStatus(ApplicationStatus.SUBMITTED);

        Application app2 = createTestApplication(testJob);
        app2.setStatus(ApplicationStatus.UNDER_REVIEW);

        applicationRepository.save(app1);
        applicationRepository.save(app2);

        // Get stats
        ApplicationStatsResponse stats = applicationService.getApplicationStats(email);

        // Verify stats
        assertEquals(2, stats.getTotalApplications());
        assertEquals(1, stats.getApplicationsByStatus().get(ApplicationStatus.SUBMITTED));
        assertEquals(1, stats.getApplicationsByStatus().get(ApplicationStatus.UNDER_REVIEW));
        assertEquals(2, stats.getApplicationsByJob().get(testJob.getId()));
    }

    @Test
    void deleteApplication() {
        // Create application
        Application application = createTestApplication(testJob);
        applicationRepository.save(application);

        // Delete application
        applicationService.deleteApplication(application.getId(), email);

        // Verify deletion
        assertTrue(applicationRepository.findById(application.getId()).isEmpty());
    }

    private User createTestUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPassword("password");
        user.setEnabled(true);
        user.setLocked(false);
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    private Job createTestJob(User user) {
        Job job = new Job();
        job.setTitle("Software Engineer");
        job.setDescription("A great job opportunity");
        job.setRequirements("Java, Spring Boot");
        job.setCompanyName("Test Company");
        job.setWorkplaceType(WorkplaceType.REMOTE);
        job.setEmploymentType(EmploymentType.FULL_TIME);
        job.setMinSalary(BigDecimal.valueOf(80000));
        job.setMaxSalary(BigDecimal.valueOf(120000));
        job.setCurrency("USD");
        job.setActive(true);
        job.setVisibleUntil(LocalDateTime.now().plusDays(30));
        job.setCreatedBy(user);
        return job;
    }

    private Application createTestApplication(Job job) {
        Application application = new Application();
        application.setJob(job);
        application.setApplicantName("Test Applicant");
        application.setApplicantEmail("applicant" + UUID.randomUUID() + "@example.com");
        application.setPhoneNumber("123-456-7890");
        application.setResumeUrl("https://resume.example.com/" + UUID.randomUUID());
        application.setCoverLetter("I'm very interested in this position and would like to apply. I have extensive experience in the required skills and believe I would be a great fit for your team. Please consider my application.");
        application.setStatus(ApplicationStatus.SUBMITTED);
        return application;
    }
}