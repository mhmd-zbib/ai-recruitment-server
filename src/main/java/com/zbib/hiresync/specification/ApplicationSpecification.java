package com.zbib.hiresync.specification;

import com.zbib.hiresync.dto.filter.ApplicationFilter;
import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class ApplicationSpecification {

    public Specification<Application> buildSpecification(ApplicationFilter filter) {
        return (root, query, cb) -> {

            assert query != null;
            query.distinct(true);
            Predicate predicate = cb.conjunction();
            
            // General search across multiple fields
            if (StringUtils.hasText(filter.getSearch())) {
                String pattern = "%" + filter.getSearch().toLowerCase() + "%";
                predicate = cb.and(predicate, 
                    cb.or(
                        cb.like(cb.lower(root.get("applicantName")), pattern),
                        cb.like(cb.lower(root.get("applicantEmail")), pattern),
                        cb.like(cb.lower(root.get("phoneNumber")), pattern),
                        cb.like(cb.lower(root.get("coverLetter")), pattern)
                    )
                );
            }
            
            // Filter by job post
            if (filter.getJobPostId() != null) {
                predicate = cb.and(predicate, 
                    cb.equal(root.get("jobPost").get("id"), filter.getJobPostId())
                );
            }
            
            // Filter by applicant email
            if (StringUtils.hasText(filter.getApplicantEmail())) {
                predicate = cb.and(predicate, 
                    cb.equal(root.get("applicantEmail"), filter.getApplicantEmail())
                );
            }
            
            // Filter by status
            if (filter.getStatus() != null) {
                predicate = cb.and(predicate, 
                    cb.equal(root.get("status"), filter.getStatus())
                );
            }
            
            // Filter by multiple statuses
            if (!CollectionUtils.isEmpty(filter.getStatuses())) {
                predicate = cb.and(predicate, 
                    root.get("status").in(filter.getStatuses())
                );
            }
            
            // Filter by skills
            if (!CollectionUtils.isEmpty(filter.getSkills())) {
                Join<Object, Object> skillsJoin = root.join("skills");
                predicate = cb.and(predicate, 
                    skillsJoin.get("name").in(filter.getSkills())
                );
            }
            
            // Filter by submission date range
            if (filter.getSubmittedAfter() != null) {
                predicate = cb.and(predicate, 
                    cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getSubmittedAfter())
                );
            }
            
            if (filter.getSubmittedBefore() != null) {
                predicate = cb.and(predicate, 
                    cb.lessThanOrEqualTo(root.get("createdAt"), filter.getSubmittedBefore())
                );
            }

            return predicate;
        };
    }
    
    public Specification<Application> buildSpecificationForUserJobPosts(ApplicationFilter filter, User currentUser) {
        Specification<Application> baseSpec = buildSpecification(filter);
        Specification<Application> userJobPostsSpec = (root, query, cb) -> 
            cb.equal(root.get("jobPost").get("createdBy").get("id"), currentUser.getId());
            
        return baseSpec.and(userJobPostsSpec);
    }
} 