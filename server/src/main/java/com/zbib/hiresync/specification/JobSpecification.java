package com.zbib.hiresync.specification;

import com.zbib.hiresync.dto.JobFilter;
import com.zbib.hiresync.entity.Job;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JobSpecification {

    public static Specification<Job> buildSpecification(UUID userId, JobFilter filter) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (userId != null) {
                predicates.add(criteriaBuilder.equal(root
                        .get("user")
                        .get("id"), userId));
            }

            if (filter.getQuery() != null && !filter
                    .getQuery()
                    .isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), "%" + filter
                        .getQuery()
                        .toLowerCase() + "%"));
            }


            if (filter.getLocationType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("locationType"), filter.getLocationType()));
            }

            if (filter.getEmploymentType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("employmentType"), filter.getEmploymentType()));
            }

            if (filter.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
            }

            if (filter.getMinExperience() != null) {
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(root.get("yearsOfExperience"), filter.getMinExperience()));
            }

            if (filter.getMaxExperience() != null) {
                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(root.get("yearsOfExperience"), filter.getMaxExperience()));
            }

            if (filter.getMinSalary() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("minSalary"), filter.getMinSalary()));
            }

            if (filter.getMaxSalary() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("maxSalary"), filter.getMaxSalary()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}