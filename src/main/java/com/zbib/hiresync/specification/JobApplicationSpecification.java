package com.zbib.hiresync.specification;

import com.zbib.hiresync.dto.JobApplicationFilter;
import com.zbib.hiresync.entity.Application;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public class JobApplicationSpecification {

  public static Specification<Application> buildSpecification(
      UUID jobId, JobApplicationFilter filter) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      // Job ID filter
      if (jobId != null) {
        predicates.add(
            SpecificationUtils.createEqualPredicate(
                criteriaBuilder, root.get("job").get("id"), jobId));
      }

      // Status filter
      if (filter.getStatus() != null) {
        predicates.add(
            SpecificationUtils.createEqualPredicate(
                criteriaBuilder, root.get("status"), filter.getStatus()));
      }

      // Date range filters
      SpecificationUtils.addDateGreaterThanOrEqualPredicateIfNotNull(
          predicates, root.get("appliedAt"), filter.getMinCreatedAt(), criteriaBuilder);

      SpecificationUtils.addDateLessThanOrEqualPredicateIfNotNull(
          predicates, root.get("appliedAt"), filter.getMaxCreatedAt(), criteriaBuilder);

      // Text search across multiple fields
      SpecificationUtils.addStringSearchPredicateIfNotEmpty(
          predicates,
          criteriaBuilder,
          filter.getQuery(),
          root.get("firstName"),
          root.get("lastName"),
          root.get("email"),
          root.get("phoneNumber"));

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }
}
