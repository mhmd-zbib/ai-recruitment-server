package com.zbib.hiresync.specification;

import com.zbib.hiresync.dto.filter.ApplicationFilter;
import com.zbib.hiresync.dto.filter.SortCriteria;
import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.enums.ApplicationStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class ApplicationSpecification {

    public Specification<Application> buildSpecification(ApplicationFilter filter) {
        return (root, query, cb) -> {
            query.distinct(true);
            return buildPredicate(filter, root, cb);
        };
    }
    
    private Predicate buildPredicate(ApplicationFilter filter, Root<Application> root, CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();
            
        // Text search across multiple fields
        if (StringUtils.hasText(filter.getSearchQuery())) {
            String pattern = "%" + filter.getSearchQuery().toLowerCase() + "%";
            predicates.add(
                cb.or(
                    cb.like(cb.lower(root.get("applicantName")), pattern),
                    cb.like(cb.lower(root.get("applicantEmail")), pattern),
                    cb.like(cb.lower(root.get("phoneNumber")), pattern),
                    cb.like(cb.lower(root.get("coverLetter")), pattern)
                )
            );
        }
        
        // Job filtering
        if (filter.getJobId() != null) {
            predicates.add(cb.equal(root.get("job").get("id"), filter.getJobId()));
        }
        
        if (!CollectionUtils.isEmpty(filter.getJobIds())) {
            predicates.add(root.get("job").get("id").in(filter.getJobIds()));
        }
        
        // Status filtering
        if (filter.getStatus() != null) {
            predicates.add(cb.equal(root.get("status"), filter.getStatus()));
        }
        
        if (!CollectionUtils.isEmpty(filter.getStatuses())) {
            predicates.add(root.get("status").in(filter.getStatuses()));
        }
        
        // Skills filtering
        if (!CollectionUtils.isEmpty(filter.getSkills())) {
            Join<Object, Object> skillsJoin = root.join("skills");
            predicates.add(skillsJoin.get("name").in(filter.getSkills()));
        }
        
        // Date range filtering for submission
        if (filter.getSubmittedAfter() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getSubmittedAfter()));
        }
        
        if (filter.getSubmittedBefore() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getSubmittedBefore()));
        }
        
        // Date range filtering for modification
        if (filter.getModifiedDateFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("updatedAt"), filter.getModifiedDateFrom()));
        }
        
        if (filter.getModifiedDateTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("updatedAt"), filter.getModifiedDateTo()));
        }
        
        // Advanced filtering options
        if (filter.getMinMatchScore() != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                root.get("matchScore").as(Double.class), filter.getMinMatchScore()));
        }
        
        if (filter.getHasResume() != null) {
            if (filter.getHasResume()) {
                predicates.add(cb.and(
                    cb.isNotNull(root.get("resumeUrl")),
                    cb.notEqual(root.get("resumeUrl"), "")
                ));
            } else {
                predicates.add(cb.or(
                    cb.isNull(root.get("resumeUrl")),
                    cb.equal(root.get("resumeUrl"), "")
                ));
            }
        }
        
        if (filter.getHasPortfolio() != null) {
            if (filter.getHasPortfolio()) {
                predicates.add(cb.and(
                    cb.isNotNull(root.get("portfolioUrl")),
                    cb.notEqual(root.get("portfolioUrl"), "")
                ));
            } else {
                predicates.add(cb.or(
                    cb.isNull(root.get("portfolioUrl")),
                    cb.equal(root.get("portfolioUrl"), "")
                ));
            }
        }
        
        if (filter.getHasLinkedIn() != null) {
            if (filter.getHasLinkedIn()) {
                predicates.add(cb.and(
                    cb.isNotNull(root.get("linkedinUrl")),
                    cb.notEqual(root.get("linkedinUrl"), "")
                ));
            } else {
                predicates.add(cb.or(
                    cb.isNull(root.get("linkedinUrl")),
                    cb.equal(root.get("linkedinUrl"), "")
                ));
            }
        }
        
        if (filter.getMinCoverLetterLength() != null) {
            predicates.add(cb.and(
                cb.isNotNull(root.get("coverLetter")),
                cb.greaterThanOrEqualTo(cb.length(root.get("coverLetter")), 
                    filter.getMinCoverLetterLength())
            ));
        }
            
        return cb.and(predicates.toArray(new Predicate[0]));
    }
    
    public Specification<Application> buildSpecificationForUserJobs(ApplicationFilter filter, User currentUser) {
        return (root, query, cb) -> {
            Predicate basePredicate = buildPredicate(filter, root, cb);
            Predicate userJobPredicate = cb.equal(
                root.get("job").get("createdBy").get("id"), currentUser.getId());
                
            // Apply sorting if provided
            if (!CollectionUtils.isEmpty(filter.getSortCriteria())) {
                List<Order> orders = new ArrayList<>();
                for (SortCriteria sortCriteria : filter.getSortCriteria()) {
                    if (Sort.Direction.ASC.equals(sortCriteria.getDirection())) {
                        orders.add(cb.asc(root.get(sortCriteria.getField())));
                    } else {
                        orders.add(cb.desc(root.get(sortCriteria.getField())));
                    }
                }
                query.orderBy(orders);
            } else {
                // Default sort by created date descending
                query.orderBy(cb.desc(root.get("createdAt")));
            }
            
            return cb.and(basePredicate, userJobPredicate);
        };
    }
    
    // Utility method to add dynamic sorting
    public Sort buildSort(List<SortCriteria> sortCriteria) {
        if (CollectionUtils.isEmpty(sortCriteria)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        
        List<Sort.Order> orders = new ArrayList<>();
        for (SortCriteria criteria : sortCriteria) {
            orders.add(new Sort.Order(criteria.getDirection(), criteria.getField()));
        }
        return Sort.by(orders);
    }
} 