package com.zbib.hiresync.specification;

import com.zbib.hiresync.dto.filter.JobPostFilter;
import com.zbib.hiresync.entity.JobPost;
import com.zbib.hiresync.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Specification for querying job posts with comprehensive filter criteria
 */
@Component
public class JobPostSpecification {

    /**
     * Build a complete specification from a filter DTO
     *
     * @param filter the filter DTO with all query parameters
     * @return the combined specification with all filter conditions
     */
    public Specification<JobPost> buildSpecification(JobPostFilter filter) {
        return (root, query, cb) -> {
            // Ensure distinct results when joining
            query.distinct(true);
            
            // Start with a true condition that will be combined with AND
            Predicate predicate = cb.conjunction();
            
            // Active status filter
            if (filter.getActive() != null) {
                if (filter.getActive()) {
                    // Active posts only - both active flag is true and not expired
                    predicate = cb.and(predicate, 
                        cb.and(
                            cb.equal(root.get("active"), true),
                            cb.or(
                                cb.isNull(root.get("visibleUntil")),
                                cb.greaterThan(root.get("visibleUntil"), LocalDateTime.now())
                            )
                        )
                    );
                } else {
                    // Inactive posts only - either inactive flag or expired
                    predicate = cb.and(predicate,
                        cb.or(
                            cb.equal(root.get("active"), false),
                            cb.lessThanOrEqualTo(root.get("visibleUntil"), LocalDateTime.now())
                        )
                    );
                }
            }
            
            // Unified search (title, description, requirements, company name)
            if (StringUtils.hasText(filter.getSearch())) {
                predicate = cb.and(predicate, buildSearchPredicate(filter.getSearch()).toPredicate(root, query, cb));
            }
            
            // City filter
            if (StringUtils.hasText(filter.getCity())) {
                String pattern = "%" + filter.getCity().toLowerCase() + "%";
                predicate = cb.and(predicate, 
                    cb.like(cb.lower(root.join("address").get("city")), pattern)
                );
            }
            
            // Country filter
            if (StringUtils.hasText(filter.getCountry())) {
                String pattern = "%" + filter.getCountry().toLowerCase() + "%";
                predicate = cb.and(predicate, 
                    cb.like(cb.lower(root.join("address").get("country")), pattern)
                );
            }
            
            // Workplace type filter
            if (filter.getWorkplaceType() != null) {
                predicate = cb.and(predicate, 
                    cb.equal(root.get("workplaceType"), filter.getWorkplaceType())
                );
            }
            
            // Employment type filter
            if (filter.getEmploymentType() != null) {
                predicate = cb.and(predicate, 
                    cb.equal(root.get("employmentType"), filter.getEmploymentType())
                );
            }
            
            // Minimum salary filter
            if (filter.getMinSalary() != null) {
                predicate = cb.and(predicate, 
                    cb.greaterThanOrEqualTo(root.get("minSalary"), filter.getMinSalary())
                );
            }
            
            // Maximum salary filter
            if (filter.getMaxSalary() != null) {
                predicate = cb.and(predicate, 
                    cb.lessThanOrEqualTo(root.get("maxSalary"), filter.getMaxSalary())
                );
            }
            
            // Skills filter
            if (!CollectionUtils.isEmpty(filter.getSkills())) {
                Join<Object, Object> skillsJoin = root.join("skills");
                predicate = cb.and(predicate, 
                    skillsJoin.get("name").in(filter.getSkills())
                );
            }
            
            // Tags filter
            if (!CollectionUtils.isEmpty(filter.getTags())) {
                Join<Object, Object> tagsJoin = root.join("tags");
                predicate = cb.and(predicate, 
                    tagsJoin.get("name").in(filter.getTags())
                );
            }
            
            // Created by filter (if a user is specified)
            if (filter.getCreatedBy() != null) {
                predicate = cb.and(predicate,
                    cb.equal(root.get("createdBy"), filter.getCreatedBy())
                );
            }
            
            return predicate;
        };
    }
    
    /**
     * Create a specification for searching across multiple fields
     * Splits the search query into words and searches in title, description, requirements, company name
     * 
     * @param search the search query containing words to search for
     * @return specification for matching search terms across multiple fields
     */
    public Specification<JobPost> buildSearchPredicate(String search) {
        return (root, criteriaQuery, cb) -> {
            if (!StringUtils.hasText(search)) {
                return cb.conjunction();
            }
            
            // Split the search into words
            String[] words = search.toLowerCase().split("\\s+");
            List<Predicate> wordPredicates = new ArrayList<>();
            
            // For each word, create a condition that it appears in any of the searchable fields
            for (String word : words) {
                if (StringUtils.hasText(word)) {
                    String pattern = "%" + word + "%";
                    wordPredicates.add(
                        cb.or(
                            // Primary search fields
                            cb.like(cb.lower(root.get("title")), pattern),
                            cb.like(cb.lower(root.get("description")), pattern),
                            // Secondary search fields
                            cb.like(cb.lower(root.get("requirements")), pattern),
                            cb.like(cb.lower(root.get("companyName")), pattern)
                        )
                    );
                }
            }
            
            // Combine all word conditions with AND (all words must be present)
            return cb.and(wordPredicates.toArray(new Predicate[0]));
        };
    }
    
    /**
     * Create a specification to filter posts by creator
     * 
     * @param user the creator user
     * @return specification for posts created by the user
     */
    public Specification<JobPost> createdBy(User user) {
        return (root, query, cb) -> {
            if (user == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("createdBy"), user);
        };
    }
    
    /**
     * Filter for active job posts
     *
     * @return the specification
     */
    public Specification<JobPost> isActive() {
        // Create a filter with just the active status set to true
        JobPostFilter filter = new JobPostFilter();
        filter.setActive(true);
        return buildSpecification(filter);
    }
}