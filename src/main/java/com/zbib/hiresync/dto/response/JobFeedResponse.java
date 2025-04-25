package com.zbib.hiresync.dto.response;

import com.zbib.hiresync.dto.filter.JobFilter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Response containing job feed data with search results and facets
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobFeedResponse {
    
    // Search results
    private Page<JobPostSummaryResponse> jobs;
    
    // Applied filters
    private JobFilter appliedFilters;
    
    // Facet information for filtering UI
    private Map<String, Long> locationFacets;
    private Map<String, Long> employmentTypeFacets;
    private Map<String, Long> workplaceTypeFacets;
    private Map<String, Long> skillFacets;
    private Map<String, Long> tagFacets;
    
    // Salary ranges for the UI
    private List<SalaryRangeFacet> salaryRanges;
    
    // Popular searches
    private List<String> popularSearches;
    
    // Recommended job IDs based on user history/preferences
    private Set<String> recommendedJobIds;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalaryRangeFacet {
        private String label;
        private String currency;
        private String range;
        private Long count;
    }
} 