package com.zbib.hiresync.specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.zbib.hiresync.dto.ApplicationFilter;
import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.Job;

import jakarta.persistence.criteria.*;

public class ApplicationSpecification {

  public static Specification<Application> buildSpecification(
      UUID userId, ApplicationFilter filter) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      Join<Application, Job> jobJoin = root.join("job", JoinType.INNER);

      if (userId != null) {
        predicates.add(
            SpecificationUtils.createEqualPredicate(
                criteriaBuilder, jobJoin.get("user").get("id"), userId));
      }

      // Text search across multiple fields
      if (filter.getQuery() != null && !filter.getQuery().isEmpty()) {
        String searchPattern = "%" + filter.getQuery().toLowerCase() + "%";
        predicates.add(
            criteriaBuilder.or(
                SpecificationUtils.createLikePredicate(
                    criteriaBuilder, root.get("firstName"), searchPattern),
                SpecificationUtils.createLikePredicate(
                    criteriaBuilder, root.get("lastName"), searchPattern),
                SpecificationUtils.createLikePredicate(
                    criteriaBuilder, root.get("email"), searchPattern),
                SpecificationUtils.createLikePredicate(
                    criteriaBuilder, jobJoin.get("title"), searchPattern),
                SpecificationUtils.createLikePredicate(
                    criteriaBuilder, jobJoin.get("department"), searchPattern)));
      }

      // Job-related filters
      SpecificationUtils.addPredicateIfNotEmpty(predicates, jobJoin.get("id"), filter.getJobId());
      SpecificationUtils.addPredicateIfNotEmpty(
          predicates, jobJoin.get("locationType"), filter.getLocationType());
      SpecificationUtils.addPredicateIfNotEmpty(
          predicates, jobJoin.get("employmentType"), filter.getEmploymentType());
      SpecificationUtils.addPredicateIfNotEmpty(
          predicates, jobJoin.get("status"), filter.getJobStatus());
      SpecificationUtils.addPredicateIfNotEmpty(predicates, root.get("status"), filter.getStatus());

      // Experience and salary range filters
      SpecificationUtils.addGreaterThanOrEqualPredicateIfNotNull(
          predicates, jobJoin.get("yearsOfExperience"), filter.getMinExperience(), criteriaBuilder);
      SpecificationUtils.addLessThanOrEqualPredicateIfNotNull(
          predicates, jobJoin.get("yearsOfExperience"), filter.getMaxExperience(), criteriaBuilder);

      SpecificationUtils.addGreaterThanOrEqualPredicateIfNotNull(
          predicates, jobJoin.get("minSalary"), filter.getMinSalary(), criteriaBuilder);
      SpecificationUtils.addLessThanOrEqualPredicateIfNotNull(
          predicates, jobJoin.get("maxSalary"), filter.getMaxSalary(), criteriaBuilder);

      // Date filters
      SpecificationUtils.addDateGreaterThanOrEqualPredicateIfNotNull(
          predicates, jobJoin.get("createdAt"), filter.getMinJobCreatedAt(), criteriaBuilder);
      SpecificationUtils.addDateLessThanOrEqualPredicateIfNotNull(
          predicates, jobJoin.get("createdAt"), filter.getMaxJobCreatedAt(), criteriaBuilder);

      SpecificationUtils.addDateGreaterThanOrEqualPredicateIfNotNull(
          predicates, root.get("createdAt"), filter.getMinCreatedAt(), criteriaBuilder);
      SpecificationUtils.addDateLessThanOrEqualPredicateIfNotNull(
          predicates, root.get("createdAt"), filter.getMaxCreatedAt(), criteriaBuilder);

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }
}
