package com.zbib.hiresync.specification;

import com.zbib.hiresync.dto.ApplicationFilter;
import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.Job;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ApplicationSpecification {

    public static Specification<Application> buildSpecification(UUID userId, ApplicationFilter filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<Application, Job> jobJoin = root.join("job", JoinType.INNER);

            if (userId != null) {
                predicates.add(criteriaBuilder.equal(jobJoin
                        .get("user")
                        .get("id"), userId));
            }

            if (StringUtils.hasText(filter.getQuery())) {
                String searchPattern = "%" + filter
                        .getQuery()
                        .toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(jobJoin.get("title")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(jobJoin.get("department")), searchPattern)
                ));
            }

            // Job-related filters
            if (!CollectionUtils.isEmpty(filter.getJobId())) {
                predicates.add(jobJoin
                        .get("id")
                        .in(filter.getJobId()));
            }

            if (!CollectionUtils.isEmpty(filter.getLocationType())) {
                predicates.add(jobJoin
                        .get("locationType")
                        .in(filter.getLocationType()));
            }

            if (!CollectionUtils.isEmpty(filter.getEmploymentType())) {
                predicates.add(jobJoin
                        .get("employmentType")
                        .in(filter.getEmploymentType()));
            }

            if (!CollectionUtils.isEmpty(filter.getJobStatus())) {
                predicates.add(jobJoin
                        .get("status")
                        .in(filter.getJobStatus()));
            }

            // Experience and salary ranges
            if (filter.getMinExperience() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(jobJoin.get("yearsOfExperience"),
                        filter.getMinExperience()));
            }

            if (filter.getMaxExperience() != null) {
                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(jobJoin.get("yearsOfExperience"), filter.getMaxExperience()));
            }

            if (filter.getMinSalary() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(jobJoin.get("minSalary"), filter.getMinSalary()));
            }

            if (filter.getMaxSalary() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(jobJoin.get("maxSalary"), filter.getMaxSalary()));
            }

            // Application status filters
            if (!CollectionUtils.isEmpty(filter.getStatus())) {
                predicates.add(root
                        .get("status")
                        .in(filter.getStatus()));
            }

            // Date filters for job creation
            if (filter.getMinJobCreatedAt() != null) {
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(jobJoin.get("createdAt"), filter.getMinJobCreatedAt()));
            }

            if (filter.getMaxJobCreatedAt() != null) {
                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(jobJoin.get("createdAt"), filter.getMaxJobCreatedAt()));
            }

            // Date filters for application creation
            if (filter.getMinCreatedAt() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), filter.getMinCreatedAt()));
            }

            if (filter.getMaxCreatedAt() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), filter.getMaxCreatedAt()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}