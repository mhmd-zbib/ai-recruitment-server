package com.zbib.hiresync.specification;

import com.zbib.hiresync.dto.JobApplicationFilter;
import com.zbib.hiresync.entity.Application;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JobApplicationSpecification {

    public static Specification<Application> buildSpecification(UUID jobId, JobApplicationFilter filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (jobId != null) {
                predicates.add(criteriaBuilder.equal(root
                        .get("job")
                        .get("id"), jobId));
            }

            if (filter.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
            }

            if (filter.getMinCreatedAt() != null) {
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(root.get("appliedAt"), filter.getMinCreatedAt()));
            }

            if (filter.getMaxCreatedAt() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("appliedAt"), filter.getMaxCreatedAt()));
            }

            if (StringUtils.hasText(filter.getQuery())) {
                String searchTerm = "%" + filter
                        .getQuery()
                        .toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), searchTerm),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), searchTerm),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), searchTerm),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("phoneNumber")), searchTerm)

                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}