package com.zbib.hiresync.specification;

import com.zbib.hiresync.dto.filter.ApplicationFilter;
import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.Skill;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class ApplicationSpecification {

    public Specification<Application> buildSpecification(ApplicationFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (filter.getJobId() != null) {
                predicates.add(cb.equal(root.get("job").get("id"), filter.getJobId()));
            }
            
            if (filter.getSearchQuery() != null && !filter.getSearchQuery().isBlank()) {
                String searchTerm = "%" + filter.getSearchQuery().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("applicantName")), searchTerm),
                    cb.like(cb.lower(root.get("applicantEmail")), searchTerm),
                    cb.like(cb.lower(root.get("coverLetter")), searchTerm),
                    cb.like(cb.lower(root.get("notes")), searchTerm)
                ));
            }
            
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            
            if (filter.getStatuses() != null && !filter.getStatuses().isEmpty()) {
                predicates.add(root.get("status").in(filter.getStatuses()));
            }
            
            if (filter.getSkills() != null && !filter.getSkills().isEmpty()) {
                Join<Application, Skill> skillJoin = root.join("skills");
                predicates.add(skillJoin.get("name").in(filter.getSkills()));
            }
            
            if (filter.getAppliedWithinDays() != null) {
                LocalDateTime cutoffDate = LocalDateTime.now().minusDays(filter.getAppliedWithinDays());
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), cutoffDate));
            }
            
            if (filter.getCreatedAfter() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getCreatedAfter()));
            }
            
            if (filter.getCreatedBefore() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getCreatedBefore()));
            }
            
            if (filter.getUpdatedAfter() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("updatedAt"), filter.getUpdatedAfter()));
            }
            
            if (filter.getUpdatedBefore() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("updatedAt"), filter.getUpdatedBefore()));
            }
            
            if (filter.getApplicantEmail() != null && !filter.getApplicantEmail().isBlank()) {
                predicates.add(cb.equal(root.get("applicantEmail"), filter.getApplicantEmail()));
            }
            
            if (filter.getPhoneNumber() != null && !filter.getPhoneNumber().isBlank()) {
                predicates.add(cb.like(root.get("phoneNumber"), "%" + filter.getPhoneNumber() + "%"));
            }
            
            query.distinct(true);
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}