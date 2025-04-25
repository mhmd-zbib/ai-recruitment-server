package com.zbib.hiresync.specification;

import com.zbib.hiresync.dto.filter.JobFilter;
import com.zbib.hiresync.entity.Job;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class JobSpecification {

    public Specification<Job> buildSpecification(JobFilter filter) {
        Specification<Job> spec = Specification.where(null);
        
        // Apply active filter based on context
        if (filter.getIsFeedRequest() != null && filter.getIsFeedRequest()) {
            spec = spec.and(isActive());
        } else {
            if (filter.getActive() != null) {
                if (filter.getActive()) {
                    spec = spec.and(isActive());
                } else {
                    spec = spec.and((root, query, cb) -> 
                        cb.or(
                            cb.equal(root.get("active"), false),
                            cb.lessThanOrEqualTo(root.get("visibleUntil"), LocalDateTime.now())
                        )
                    );
                }
            } else if (filter.getIncludeInactive() == null || !filter.getIncludeInactive()) {
                spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), true));
            }
        }
        
        // Add search query if present
        if (StringUtils.hasText(filter.getSearchQuery())) {
            spec = spec.and((root, query, cb) -> 
                buildSearchPredicate(root, cb, filter.getSearchQuery())
            );
        }
        
        // Add location filters
        if (StringUtils.hasText(filter.getCity())) {
            spec = spec.and(cityContains(filter.getCity()));
        }
        
        if (StringUtils.hasText(filter.getCountry())) {
            spec = spec.and(countryContains(filter.getCountry()));
        }
        
        // Add remote filter
        if (filter.getRemoteAllowed() != null && filter.getRemoteAllowed()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("workplaceType"), "REMOTE"));
        }
        
        // Add salary range filter
        spec = spec.and(salaryRange(filter.getMinSalary(), filter.getMaxSalary(), filter.getCurrency()));
        
        // Add employment type filter
        if (!CollectionUtils.isEmpty(filter.getEmploymentTypes())) {
            spec = spec.and((root, query, cb) -> root.get("employmentType").in(filter.getEmploymentTypes()));
        }
        
        // Add workplace type filter
        if (!CollectionUtils.isEmpty(filter.getWorkplaceTypes())) {
            spec = spec.and((root, query, cb) -> root.get("workplaceType").in(filter.getWorkplaceTypes()));
        }
        
        // Add skills filter
        if (!CollectionUtils.isEmpty(filter.getSkills())) {
            spec = spec.and(hasSkills(filter.getSkills()));
        }
        
        // Add tags filter
        if (!CollectionUtils.isEmpty(filter.getTags())) {
            spec = spec.and(hasTags(filter.getTags()));
        }
        
        // Add date range filter
        if (filter.getPostedWithinDays() != null) {
            spec = spec.and(createdWithinDays(filter.getPostedWithinDays()));
        }
        
        // Add visibility date filters
        if (filter.getVisibleAfter() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.or(
                    cb.isNull(root.get("visibleUntil")),
                    cb.greaterThanOrEqualTo(root.get("visibleUntil"), filter.getVisibleAfter())
                )
            );
        }
        
        if (filter.getVisibleBefore() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.lessThanOrEqualTo(root.get("createdAt"), filter.getVisibleBefore())
            );
        }
        
        // Add creation date filters
        if (filter.getCreatedAfter() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getCreatedAfter())
            );
        }
        
        if (filter.getCreatedBefore() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.lessThanOrEqualTo(root.get("createdAt"), filter.getCreatedBefore())
            );
        }
        
        // Add creator filter for HR view
        if (filter.getCreatedById() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("createdBy").get("id"), filter.getCreatedById())
            );
        }
        
        return spec;
    }
    
    public double calculateRelevanceScore(Job job, String searchQuery) {
        if (job == null || searchQuery == null || searchQuery.isEmpty()) {
            return 0.0;
        }
        
        final String searchQueryLower = searchQuery.toLowerCase();
        double score = 0.0;
        
        // Higher weight for title matches
        if (job.getTitle() != null && job.getTitle().toLowerCase().contains(searchQueryLower)) {
            score += 5.0;
        }
        
        // Medium weight for company name
        if (job.getCompanyName() != null && job.getCompanyName().toLowerCase().contains(searchQueryLower)) {
            score += 3.0;
        }
        
        // Lower weight for description and requirements
        if (job.getDescription() != null && job.getDescription().toLowerCase().contains(searchQueryLower)) {
            score += 2.0;
        }
        
        if (job.getRequirements() != null && job.getRequirements().toLowerCase().contains(searchQueryLower)) {
            score += 1.5;
        }
        
        // Check skills and tags too
        boolean skillMatch = job.getSkills().stream()
                .anyMatch(skill -> skill.getName().toLowerCase().contains(searchQueryLower));
                
        boolean tagMatch = job.getTags().stream()
                .anyMatch(tag -> tag.getName().toLowerCase().contains(searchQueryLower));
                
        if (skillMatch) score += 4.0;
        if (tagMatch) score += 3.0;
        
        return score;
    }
    
    private Specification<Job> isActive() {
        return (root, query, cb) -> 
            cb.and(
                cb.equal(root.get("active"), true),
                cb.or(
                    cb.isNull(root.get("visibleUntil")),
                    cb.greaterThan(root.get("visibleUntil"), LocalDateTime.now())
                )
            );
    }
    
    private Predicate buildSearchPredicate(Root<Job> root, CriteriaBuilder cb, String searchQuery) {
        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            return cb.conjunction();
        }
        
        String pattern = "%" + searchQuery.toLowerCase() + "%";
        return cb.or(
            cb.like(cb.lower(root.get("title")), pattern),
            cb.like(cb.lower(root.get("description")), pattern),
            cb.like(cb.lower(root.get("companyName")), pattern),
            cb.like(cb.lower(root.get("requirements")), pattern)
        );
    }
    
    private Specification<Job> cityContains(String city) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(city)) {
                return cb.conjunction();
            }
            
            String pattern = "%" + city.toLowerCase() + "%";
            Join<Object, Object> addressJoin = root.join("address", JoinType.LEFT);
            return cb.like(cb.lower(addressJoin.get("city")), pattern);
        };
    }
    
    private Specification<Job> countryContains(String country) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(country)) {
                return cb.conjunction();
            }
            
            String pattern = "%" + country.toLowerCase() + "%";
            Join<Object, Object> addressJoin = root.join("address", JoinType.LEFT);
            return cb.like(cb.lower(addressJoin.get("country")), pattern);
        };
    }
    
    private Specification<Job> hasSkills(Iterable<String> skills) {
        return (root, query, cb) -> {
            if (CollectionUtils.isEmpty((List<String>)skills)) {
                return cb.conjunction();
            }
            
            Join<Object, Object> skillsJoin = root.join("skills");
            return skillsJoin.get("name").in(skills);
        };
    }
    
    private Specification<Job> hasTags(Iterable<String> tags) {
        return (root, query, cb) -> {
            if (CollectionUtils.isEmpty((List<String>)tags)) {
                return cb.conjunction();
            }
            
            Join<Object, Object> tagsJoin = root.join("tags");
            return tagsJoin.get("name").in(tags);
        };
    }
    
    private Specification<Job> salaryRange(BigDecimal min, BigDecimal max, String currency) {
        return (root, query, cb) -> {
            if ((min == null && max == null) || currency == null) {
                return cb.conjunction();
            }
            
            // Match jobs with the correct currency
            var currencyMatch = cb.equal(root.get("currency"), currency);
            
            // For minimum salary, compare with the job's max salary
            if (min != null && max == null) {
                return cb.and(
                    currencyMatch,
                    cb.greaterThanOrEqualTo(root.get("maxSalary"), min)
                );
            }
            
            // For maximum salary, compare with the job's min salary
            if (max != null && min == null) {
                return cb.and(
                    currencyMatch,
                    cb.lessThanOrEqualTo(root.get("minSalary"), max)
                );
            }
            
            // For both min and max, check for overlap of the ranges
            return cb.and(
                cb.and(
                    currencyMatch,
                    cb.lessThanOrEqualTo(root.get("minSalary"), max)
                ),
                cb.greaterThanOrEqualTo(root.get("maxSalary"), min)
            );
        };
    }
    
    private Specification<Job> createdWithinDays(Integer days) {
        return (root, query, cb) -> {
            if (days == null || days <= 0) {
                return cb.conjunction();
            }
            
            return cb.greaterThanOrEqualTo(
                root.get("createdAt"),
                LocalDateTime.now().minusDays(days)
            );
        };
    }
}