package com.zbib.hiresync.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zbib.hiresync.config.TestSecurityConfig;
import com.zbib.hiresync.dto.filter.ApplicationFilter;
import com.zbib.hiresync.dto.filter.JobFilter;
import com.zbib.hiresync.dto.request.CreateJobPostRequest;
import com.zbib.hiresync.dto.request.UpdateJobPostRequest;
import com.zbib.hiresync.dto.response.ApplicationSummaryResponse;
import com.zbib.hiresync.dto.response.JobFeedResponse;
import com.zbib.hiresync.dto.response.JobPostResponse;
import com.zbib.hiresync.dto.response.JobPostStatsResponse;
import com.zbib.hiresync.dto.response.JobPostSummaryResponse;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.enums.ApplicationStatus;
import com.zbib.hiresync.enums.EmploymentType;
import com.zbib.hiresync.enums.WorkplaceType;
import com.zbib.hiresync.service.ApplicationService;
import com.zbib.hiresync.service.AuthService;
import com.zbib.hiresync.service.JobService;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

@WebMvcTest(JobController.class)
@Import(TestSecurityConfig.class)
public class JobControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JobService jobService;

    @MockBean
    private ApplicationService applicationService;

    @MockBean
    private AuthService authService;

    private JobPostResponse jobPostResponse;
    private List<JobPostSummaryResponse> summaryResponses;
    private JobFeedResponse feedResponse;
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
        
        // Setup JobFeed response
        Map<String, Long> locationFacets = new HashMap<>();
        locationFacets.put("country:US", 10L);
        locationFacets.put("city:New York", 5L);
        
        Map<String, Long> skillFacets = new HashMap<>();
        skillFacets.put("Java", 10L);
        skillFacets.put("Spring Boot", 8L);
        
        List<JobFeedResponse.SalaryRangeFacet> salaryRanges = List.of(
            new JobFeedResponse.SalaryRangeFacet("50k - 100k", "USD", "50k - 100k USD", 5L),
            new JobFeedResponse.SalaryRangeFacet("100k+", "USD", "100k+ USD", 3L)
        );
        
        Page<JobPostSummaryResponse> jobsPage = new PageImpl<>(summaryResponses);
        
        feedResponse = JobFeedResponse.builder()
                .jobs(jobsPage)
                .appliedFilters(new JobFilter())
                .locationFacets(locationFacets)
                .skillFacets(skillFacets)
                .salaryRanges(salaryRanges)
                .popularSearches(List.of("Software Engineer", "Marketing"))
                .recommendedJobIds(Set.of())
                .build();
    }

    // Public endpoints tests
    
    @Test
    public void testGetJobFeed() throws Exception {
        // Given
        when(jobService.getJobFeed(any(JobFilter.class), any(Pageable.class))).thenReturn(feedResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/jobs/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs").exists())
                .andExpect(jsonPath("$.locationFacets").exists())
                .andExpect(jsonPath("$.skillFacets").exists())
                .andExpect(jsonPath("$.salaryRanges").exists());
    }
    
    @Test
    public void testGetPublicJobPosts() throws Exception {
        // Given
        PageImpl<JobPostSummaryResponse> page = new PageImpl<>(summaryResponses);
        when(jobService.getAllPublicJobPosts(any(JobFilter.class), any(Pageable.class))).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/v1/jobs/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(summaryResponses.size())));
    }
    
    @Test
    public void testGetPublicJobPostById() throws Exception {
        // Given
        UUID jobPostId = UUID.randomUUID();
        when(jobService.getPublicJobPostById(jobPostId)).thenReturn(jobPostResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/jobs/public/" + jobPostId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Software Engineer"));
    }

    // Authenticated endpoints tests
    
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

        when(jobService.createJobPost(any(CreateJobPostRequest.class), eq(TEST_USERNAME))).thenReturn(jobPostResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/jobs")
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

        when(jobService.updateJobPost(eq(jobPostId), any(UpdateJobPostRequest.class), eq(TEST_USERNAME))).thenReturn(jobPostResponse);

        // When & Then
        mockMvc.perform(put("/api/v1/jobs/" + jobPostId)
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
        when(jobService.getJobPostById(jobPostId, TEST_USERNAME)).thenReturn(jobPostResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/jobs/" + jobPostId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Software Engineer"));
    }

    @Test
    @WithMockUser(username = TEST_USERNAME)
    public void testDeleteJobPost() throws Exception {
        // Given
        UUID jobPostId = UUID.randomUUID();
        doNothing().when(jobService).deleteJobPost(jobPostId, TEST_USERNAME);

        // When & Then
        mockMvc.perform(delete("/api/v1/jobs/" + jobPostId))
                .andExpect(status().isNoContent());

        verify(jobService, times(1)).deleteJobPost(jobPostId, TEST_USERNAME);
    }

    @Test
    @WithMockUser(username = TEST_USERNAME)
    public void testGetHrJobPosts() throws Exception {
        // Given
        PageImpl<JobPostSummaryResponse> page = new PageImpl<>(summaryResponses);
        when(jobService.getHrJobPosts(eq(TEST_USERNAME), any(JobFilter.class), any(Pageable.class))).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(summaryResponses.size())));
    }

    @Test
    @WithMockUser(username = TEST_USERNAME)
    public void testToggleJobPostStatus() throws Exception {
        // Given
        UUID jobPostId = UUID.randomUUID();
        when(jobService.toggleJobPostActiveStatus(jobPostId, TEST_USERNAME)).thenReturn(jobPostResponse);

        // When & Then
        mockMvc.perform(patch("/api/v1/jobs/" + jobPostId + "/toggle-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
    }
    
    @Test
    @WithMockUser(username = TEST_USERNAME)
    public void testExtendJobPostVisibility() throws Exception {
        // Given
        UUID jobPostId = UUID.randomUUID();
        int days = 30;
        when(jobService.extendJobPostVisibility(jobPostId, TEST_USERNAME, days)).thenReturn(jobPostResponse);

        // When & Then
        mockMvc.perform(patch("/api/v1/jobs/" + jobPostId + "/extend-visibility")
                .param("days", String.valueOf(days)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
    }
    
    @Test
    @WithMockUser(username = TEST_USERNAME)
    public void testGetJobPostStats() throws Exception {
        // Given
        UUID jobPostId = UUID.randomUUID();
        JobPostStatsResponse statsResponse = JobPostStatsResponse.builder()
                .jobPostId(jobPostId)
                .jobTitle("Software Engineer")
                .totalApplications(10L)
                .applicationsByStatus(Map.of(ApplicationStatus.PENDING, 5L, ApplicationStatus.REVIEWING, 5L))
                .build();
        
        when(jobService.getJobPostStats(jobPostId, TEST_USERNAME)).thenReturn(statsResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/jobs/" + jobPostId + "/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobPostId").value(jobPostId.toString()))
                .andExpect(jsonPath("$.totalApplications").value(10));
    }
    
    @Test
    @WithMockUser(username = TEST_USERNAME)
    public void testGetJobPostsExpiringSoon() throws Exception {
        // Given
        when(jobService.getJobPostsExpiringSoon(eq(TEST_USERNAME), anyInt())).thenReturn(summaryResponses);

        // When & Then
        mockMvc.perform(get("/api/v1/jobs/expiring-soon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(summaryResponses.size())));
    }

    @Test
    @WithMockUser(username = TEST_USERNAME, roles = {"EMPLOYER"})
    public void testGetJobApplications() throws Exception {
        // Given
        UUID jobPostId = UUID.randomUUID();
        List<ApplicationSummaryResponse> applications = List.of(
            ApplicationSummaryResponse.builder()
                .id(UUID.randomUUID())
                .jobPostId(jobPostId)
                .candidateName("Jane Doe")
                .status(ApplicationStatus.PENDING)
                .appliedAt(LocalDateTime.now())
                .build()
        );
        
        Page<ApplicationSummaryResponse> applicationsPage = new PageImpl<>(applications);
        
        when(applicationService.getApplicationsByJobPostId(
                eq(jobPostId), any(ApplicationFilter.class), any(Pageable.class), eq(TEST_USERNAME)))
                .thenReturn(applicationsPage);

        // When & Then
        mockMvc.perform(get("/api/v1/jobs/" + jobPostId + "/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }
} 