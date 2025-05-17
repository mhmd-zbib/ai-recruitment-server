package com.zbib.hiresync.specification;

import com.zbib.hiresync.dto.filter.JobFilter;
import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.entity.Skill;
import com.zbib.hiresync.entity.Tag;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class JobSpecification {

    public Specification<Job> buildSpecification(JobFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (filter.getSearchQuery() != null && !filter.getSearchQuery().isBlank()) {
                String searchTerm = "%" + filter.getSearchQuery().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("title")), searchTerm),
                    cb.like(cb.lower(root.get("description")), searchTerm),
                    cb.like(cb.lower(root.get("requirements")), searchTerm),
                    cb.like(cb.lower(root.get("companyName")), searchTerm)
                ));
            }
            
            if (filter.getCity() != null && !filter.getCity().isBlank()) {
                predicates.add(cb.like(
                    cb.lower(root.get("address").get("city")), 
                    "%" + filter.getCity().toLowerCase() + "%"
                ));
            }
            
            if (filter.getCountry() != null && !filter.getCountry().isBlank()) {
                predicates.add(cb.like(
                    cb.lower(root.get("address").get("country")), 
                    "%" + filter.getCountry().toLowerCase() + "%"
                ));
            }
            
            if (filter.getRemoteAllowed() != null && filter.getRemoteAllowed()) {
                predicates.add(cb.equal(root.get("workplaceType"), "REMOTE"));
            }
            
            if (filter.getMinSalary() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("maxSalary"), filter.getMinSalary()));
            }
            
            if (filter.getMaxSalary() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("minSalary"), filter.getMaxSalary()));
            }
            
            if (filter.getCurrency() != null && !filter.getCurrency().isBlank()) {
                predicates.add(cb.equal(root.get("currency"), filter.getCurrency()));
            }
            
            if (filter.getEmploymentTypes() != null && !filter.getEmploymentTypes().isEmpty()) {
                predicates.add(root.get("employmentType").in(filter.getEmploymentTypes()));
            }
            
            if (filter.getWorkplaceTypes() != null && !filter.getWorkplaceTypes().isEmpty()) {
                predicates.add(root.get("workplaceType").in(filter.getWorkplaceTypes()));
            }
            
            if (filter.getSkills() != null && !filter.getSkills().isEmpty()) {
                Join<Job, Skill> skillJoin = root.join("skills");
                predicates.add(skillJoin.get("name").in(filter.getSkills()));
            }
            
            if (filter.getTags() != null && !filter.getTags().isEmpty()) {
                Join<Job, Tag> tagJoin = root.join("tags");
                predicates.add(tagJoin.get("name").in(filter.getTags()));
            }
            
            if (filter.getPostedWithinDays() != null) {
                LocalDateTime cutoffDate = LocalDateTime.now().minusDays(filter.getPostedWithinDays());
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), cutoffDate));
            }
            
            if (filter.getCreatedAfter() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getCreatedAfter()));
            }
            
            if (filter.getCreatedBefore() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getCreatedBefore()));
            }
            
            if (filter.getVisibleAfter() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("visibleUntil"), filter.getVisibleAfter()));
            }
            
            if (filter.getVisibleBefore() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("visibleUntil"), filter.getVisibleBefore()));
            }
            
            if (filter.getActive() != null) {
                predicates.add(cb.equal(root.get("active"), filter.getActive()));
                
                if (filter.getActive()) {
                    predicates.add(cb.or(
                        cb.isNull(root.get("visibleUntil")),
                        cb.greaterThan(root.get("visibleUntil"), LocalDateTime.now())
                    ));
                }
            } else if (filter.getIncludeInactive() == null || !filter.getIncludeInactive()) {
                predicates.add(cb.equal(root.get("active"), true));
                predicates.add(cb.or(
                    cb.isNull(root.get("visibleUntil")),
                    cb.greaterThan(root.get("visibleUntil"), LocalDateTime.now())
                ));
            }
            
            if (filter.getCreatedById() != null) {
                predicates.add(cb.equal(root.get("createdBy").get("id"), filter.getCreatedById()));
            }
            
            query.distinct(true);
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}