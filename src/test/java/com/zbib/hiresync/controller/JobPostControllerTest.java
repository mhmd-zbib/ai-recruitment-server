package com.zbib.hiresync.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zbib.hiresync.config.TestSecurityConfig;
import com.zbib.hiresync.dto.filter.ApplicationFilter;
import com.zbib.hiresync.dto.filter.JobPostFilter;
import com.zbib.hiresync.dto.request.CreateJobPostRequest;
import com.zbib.hiresync.dto.request.UpdateJobPostRequest;
import com.zbib.hiresync.dto.response.ApplicationSummaryResponse;
import com.zbib.hiresync.dto.response.JobPostResponse;
import com.zbib.hiresync.dto.response.JobPostSummaryResponse;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.enums.ApplicationStatus;
import com.zbib.hiresync.enums.EmploymentType;
import com.zbib.hiresync.enums.WorkplaceType;
import com.zbib.hiresync.service.ApplicationService;
import com.zbib.hiresync.service.AuthService;
import com.zbib.hiresync.service.JobPostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

@WebMvcTest(JobPostController.class)
@Import(TestSecurityConfig.class)
public class JobPostControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JobPostService jobPostService;

    @MockBean
    private ApplicationService applicationService;

    @MockBean
    private AuthService authService;

    private JobPostResponse jobPostResponse;
    private List<JobPostSummaryResponse> summaryResponses;
    private User mockUser;
    private static final String TEST_USERNAME = "test@example.com";

    @BeforeEach
    public void setup() {
        mockMvc = webAppContextSetup(context).build();

        // Create mock user 
        mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail(TEST_USERNAME);
        mockUser.setFirstName("Test");
        mockUser.setLastName("User");
        mockUser.setRole("EMPLOYER");
        
        // Setup test data
        jobPostResponse = JobPostResponse.builder()
                .id(UUID.randomUUID())
                .title("Software Engineer")
                .description("Job description")
                .requirements("Job requirements")
                .companyName("Tech Company")
                .location("New York, NY")
                .workplaceType(WorkplaceType.HYBRID)
                .employmentType(EmploymentType.FULL_TIME)
                .minSalary(new BigDecimal("80000.00"))
                .maxSalary(new BigDecimal("120000.00"))
                .currency("USD")
                .salaryFormatted("80000.00 - 120000.00 USD")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .visibleUntil(LocalDateTime.now().plusMonths(1))
                .createdById(UUID.randomUUID())
                .createdByName("John Doe")
                .skills(new HashSet<>(Arrays.asList("Java", "Spring Boot")))
                .tags(new HashSet<>(Arrays.asList("Backend", "Senior")))
                .build();

        JobPostSummaryResponse summaryResponse = JobPostSummaryResponse.builder()
                .id(UUID.randomUUID())
                .title("Software Engineer")
                .companyName("Tech Company")
                .location("New York, NY")
                .workplaceType(WorkplaceType.HYBRID)
                .employmentType(EmploymentType.FULL_TIME)
                .salaryFormatted("80000.00 - 120000.00 USD")
                .active(true)
                .visibleUntil(LocalDateTime.now().plusMonths(1))
                .createdAt(LocalDateTime.now())
                .createdByName("John Doe")
                .skills(new HashSet<>(Arrays.asList("Java", "Spring Boot")))
                .tags(new HashSet<>(Arrays.asList("Backend", "Senior")))
                .build();

        summaryResponses = List.of(summaryResponse);
    }

    @Test
    @WithMockUser(username = TEST_USERNAME)
    public void testCreateJobPost() throws Exception {
        // Given
        CreateJobPostRequest request = CreateJobPostRequest.builder()
                .title("Software Engineer")
                .description("Job description")
                .requirements("Job requirements")
                .companyName("Tech Company")
                .location("New York, NY")
                .workplaceType(WorkplaceType.HYBRID)
                .employmentType(EmploymentType.FULL_TIME)
                .minSalary(new BigDecimal("80000.00"))
                .maxSalary(new BigDecimal("120000.00"))
                .currency("USD")
                .active(true)
                .visibleUntil(LocalDateTime.now().plusMonths(1))
                .skills(new HashSet<>(Arrays.asList("Java", "Spring Boot")))
                .tags(new HashSet<>(Arrays.asList("Backend", "Senior")))
                .build();

        when(jobPostService.createJobPost(any(CreateJobPostRequest.class), eq(TEST_USERNAME))).thenReturn(jobPostResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/job-posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Software Engineer"))
                .andExpect(jsonPath("$.workplaceType").value("HYBRID"))
                .andExpect(jsonPath("$.employmentType").value("FULL_TIME"))
                .andExpect(jsonPath("$.skills", hasSize(2)))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser(username = TEST_USERNAME)
    public void testUpdateJobPost() throws Exception {
        // Given
        UUID jobPostId = UUID.randomUUID();
        UpdateJobPostRequest request = UpdateJobPostRequest.builder()
                .title("Updated Software Engineer")
                .description("Updated description")
                .workplaceType(WorkplaceType.REMOTE)
                .build();

        when(jobPostService.updateJobPost(eq(jobPostId), any(UpdateJobPostRequest.class), eq(TEST_USERNAME))).thenReturn(jobPostResponse);

        // When & Then
        mockMvc.perform(put("/api/v1/job-posts/" + jobPostId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @WithMockUser(username = TEST_USERNAME)
    public void testGetJobPostById() throws Exception {
        // Given
        UUID jobPostId = UUID.randomUUID();
        when(jobPostService.getJobPostById(jobPostId, TEST_USERNAME)).thenReturn(jobPostResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/job-posts/" + jobPostId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Software Engineer"));
    }

    @Test
    @WithMockUser(username = TEST_USERNAME)
    public void testDeleteJobPost() throws Exception {
        // Given
        UUID jobPostId = UUID.randomUUID();
        doNothing().when(jobPostService).deleteJobPost(jobPostId, TEST_USERNAME);

        // When & Then
        mockMvc.perform(delete("/api/v1/job-posts/" + jobPostId))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = TEST_USERNAME, roles = {"EMPLOYER"})
    public void testGetJobApplications() throws Exception {
        // Given
        UUID jobPostId = UUID.randomUUID();
        
        // Create a filter object to pass to the service method
        ApplicationFilter filter = new ApplicationFilter();
        
        // Mock application service
        Page<ApplicationSummaryResponse> applicationPage = new PageImpl<>(Arrays.asList(
            ApplicationSummaryResponse.builder()
                .id(UUID.randomUUID())
                .applicantName("John Applicant")
                .applicantEmail("john@example.com")
                .status(ApplicationStatus.SUBMITTED)
                .createdAt(LocalDateTime.now())
                .build(),
            ApplicationSummaryResponse.builder()
                .id(UUID.randomUUID())
                .applicantName("Jane Applicant")
                .applicantEmail("jane@example.com")
                .status(ApplicationStatus.UNDER_REVIEW)
                .createdAt(LocalDateTime.now())
                .build()
        ));
        
        when(applicationService.getApplicationsByJobPostId(eq(jobPostId), any(ApplicationFilter.class), any(Pageable.class), eq(TEST_USERNAME)))
            .thenReturn(applicationPage);

        // When & Then
        mockMvc.perform(get("/api/v1/job-posts/" + jobPostId + "/applications")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].applicantName").value("John Applicant"))
                .andExpect(jsonPath("$.content[1].applicantName").value("Jane Applicant"));
    }
} 