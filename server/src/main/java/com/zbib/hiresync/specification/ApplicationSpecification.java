package com.zbib.hiresync.specification;

import com.zbib.hiresync.dto.ApplicationFilter;
import com.zbib.hiresync.entity.Application;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ApplicationSpecification {

    public static Specification<Application> buildSpecification(UUID jobId, ApplicationFilter filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (jobId != null) {
                predicates.add(criteriaBuilder.equal(root.get("job").get("id"), jobId));
            }

            if (filter.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
            }

            if (filter.getAppliedDateFrom() != null) {
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(root.get("appliedAt"), filter.getAppliedDateFrom()));
            }

            if (filter.getAppliedDateTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("appliedAt"), filter.getAppliedDateTo()));
            }

            if (StringUtils.hasText(filter.getReferredBy())) {
                predicates.add(criteriaBuilder.equal(root.get("referredBy"), filter.getReferredBy()));
            }

            if (StringUtils.hasText(filter.getSearchTerm())) {
                String searchTerm = "%" + filter.getSearchTerm().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), searchTerm),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), searchTerm),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), searchTerm)
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}