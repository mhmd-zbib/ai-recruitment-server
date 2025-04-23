package com.zbib.hiresync.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zbib.hiresync.dto.filter.ApplicationFilter;
import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.JobPost;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.enums.ApplicationStatus;
import com.zbib.hiresync.enums.EmploymentType;
import com.zbib.hiresync.enums.WorkplaceType;
import com.zbib.hiresync.repository.ApplicationRepository;
import com.zbib.hiresync.repository.JobPostRepository;
import com.zbib.hiresync.repository.UserRepository;
import com.zbib.hiresync.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class JobPostApplicationsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private ApplicationRepository applicationRepository;
    
    @MockBean
    private AuthService authService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User employer;
    private JobPost jobPost;
    private Application application1;
    private Application application2;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up repositories
        applicationRepository.deleteAll();
        jobPostRepository.deleteAll();
        userRepository.deleteAll();

        // Create an employer user
        employer = createEmployer();
        
        // Set up mock for Authentication
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(employer.getEmail());
        when(SecurityContextHolder.getContext().getAuthentication()).thenReturn(authentication);
        
        // Create a job post
        jobPost = createJobPost(employer);

        // Create applications for the job post
        application1 = createApplication(jobPost, "John Applicant", "john@example.com");
        application2 = createApplication(jobPost, "Jane Applicant", "jane@example.com");
    }

    @AfterEach
    void tearDown() {
        applicationRepository.deleteAll();
        jobPostRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "employer@example.com", roles = {"EMPLOYER"})
    void getJobApplications_returnsApplicationsForJobPost() throws Exception {
        // When and Then
        mockMvc.perform(get("/v1/jobs/{id}/applications", jobPost.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].applicantEmail").exists())
                .andExpect(jsonPath("$.content[1].applicantEmail").exists());
    }

    @Test
    @WithMockUser(username = "employer@example.com", roles = {"EMPLOYER"})
    void getJobApplications_withFilterByStatus_returnsFilteredApplications() throws Exception {
        // Update application status
        application1.setStatus(ApplicationStatus.SUBMITTED);
        application2.setStatus(ApplicationStatus.UNDER_REVIEW);
        applicationRepository.save(application1);
        applicationRepository.save(application2);

        // When and Then
        mockMvc.perform(get("/v1/jobs/{id}/applications", jobPost.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .param("status", "SUBMITTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].applicantEmail").value("john@example.com"));
    }

    @Test
    @WithMockUser(username = "random@example.com", roles = {"EMPLOYER"})
    void getJobApplications_whenNotJobPostCreator_returnsUnauthorized() throws Exception {
        // Set up a different user for the random user case
        User randomUser = new User();
        randomUser.setId(UUID.randomUUID());
        randomUser.setEmail("random@example.com");
        randomUser.setFirstName("Random");
        randomUser.setLastName("User");
        randomUser.setRole("EMPLOYER");
        
        // Set up mock for Authentication with random user
        Authentication randomAuth = mock(Authentication.class);
        when(randomAuth.getName()).thenReturn(randomUser.getEmail());
        when(SecurityContextHolder.getContext().getAuthentication()).thenReturn(randomAuth);
        
        // When and Then
        mockMvc.perform(get("/v1/jobs/{id}/applications", jobPost.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    private User createEmployer() {
        User employer = new User();
        employer.setEmail("employer@example.com");
        employer.setPassword(passwordEncoder.encode("password"));
        employer.setFirstName("Employer");
        employer.setLastName("User");
        employer.setRole("EMPLOYER");
        employer.setEnabled(true);
        employer.setLocked(false);
        return userRepository.save(employer);
    }

    private JobPost createJobPost(User employer) {
        JobPost jobPost = new JobPost();
        jobPost.setTitle("Software Engineer");
        jobPost.setDescription("Job description");
        jobPost.setRequirements("Job requirements");
        jobPost.setCompanyName("Tech Company");
        jobPost.setWorkplaceType(WorkplaceType.HYBRID);
        jobPost.setEmploymentType(EmploymentType.FULL_TIME);
        jobPost.setMinSalary(new BigDecimal("80000.00"));
        jobPost.setMaxSalary(new BigDecimal("120000.00"));
        jobPost.setCurrency("USD");
        jobPost.setActive(true);
        jobPost.setVisibleUntil(LocalDateTime.now().plusMonths(1));
        jobPost.setCreatedBy(employer);
        return jobPostRepository.save(jobPost);
    }

    private Application createApplication(JobPost jobPost, String applicantName, String applicantEmail) {
        Application application = new Application();
        application.setJobPost(jobPost);
        application.setApplicantName(applicantName);
        application.setApplicantEmail(applicantEmail);
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setCoverLetter("Cover letter for " + applicantName);
        application.setResumeUrl("https://example.com/resume/" + applicantName.toLowerCase().replace(" ", "-"));
        application.setPortfolioUrl("https://example.com/portfolio/" + applicantName.toLowerCase().replace(" ", "-"));
        return applicationRepository.save(application);
    }
} 