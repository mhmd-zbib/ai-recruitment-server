package com.zbib.hiresync.dto.filter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.zbib.hiresync.enums.ApplicationStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Sort;

/**
 * Filter for job applications with various criteria
 */
@Getter
@Setter
@NoArgsConstructor
public class ApplicationFilter {
    // Basic search and filtering
    private String searchQuery;
    private UUID jobId;
    private Set<UUID> jobIds;
    private ApplicationStatus status;
    private Set<ApplicationStatus> statuses;
    private Set<String> skills;
    
    // Date ranges
    private LocalDateTime submittedAfter;
    private LocalDateTime submittedBefore;
    private LocalDateTime modifiedDateFrom;
    private LocalDateTime modifiedDateTo;
    
    // Advanced filtering
    private Double minMatchScore;
    private Boolean hasResume;
    private Boolean hasPortfolio;
    private Boolean hasLinkedIn;
    private Integer minCoverLetterLength;
    
    // Sorting - default to newest first
    private List<SortCriteria> sortCriteria = new ArrayList<>();
    
    // Helper methods for fluent API
    public ApplicationFilter withJobId(UUID jobId) {
        this.jobId = jobId;
        return this;
    }
    
    public ApplicationFilter withStatus(ApplicationStatus status) {
        this.status = status;
        return this;
    }
    
    public ApplicationFilter withStatuses(Set<ApplicationStatus> statuses) {
        this.statuses = statuses;
        return this;
    }
    
    public ApplicationFilter withSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
        return this;
    }
    
    public ApplicationFilter withSkills(Set<String> skills) {
        this.skills = skills;
        return this;
    }
    
    public ApplicationFilter withDateRange(LocalDateTime from, LocalDateTime to) {
        this.submittedAfter = from;
        this.submittedBefore = to;
        return this;
    }
    
    public ApplicationFilter withMinMatchScore(Double minMatchScore) {
        this.minMatchScore = minMatchScore;
        return this;
    }
    
    public ApplicationFilter withSort(String field, Sort.Direction direction) {
        this.sortCriteria.add(new SortCriteria(field, direction));
        return this;
    }
    
    public ApplicationFilter withHasResume(Boolean hasResume) {
        this.hasResume = hasResume;
        return this;
    }
    
    public ApplicationFilter withHasPortfolio(Boolean hasPortfolio) {
        this.hasPortfolio = hasPortfolio;
        return this;
    }
    
    public ApplicationFilter sortByCreatedAtDesc() {
        this.sortCriteria.add(SortCriteria.desc("createdAt"));
        return this;
    }
    
    public ApplicationFilter sortByMatchScoreDesc() {
        this.sortCriteria.add(SortCriteria.desc("matchScore"));
        return this;
    }
}
