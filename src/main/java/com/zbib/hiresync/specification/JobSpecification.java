package com.zbib.hiresync.specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.zbib.hiresync.dto.JobFilter;
import com.zbib.hiresync.entity.Job;

import jakarta.persistence.criteria.Predicate;

public class JobSpecification {

  public static Specification<Job> buildSpecification(UUID userId, JobFilter filter) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      // User ID filter
      if (userId != null) {
        predicates.add(
            SpecificationUtils.createEqualPredicate(
                criteriaBuilder, root.get("user").get("id"), userId));
      }

      // Text search
      SpecificationUtils.addStringSearchPredicateIfNotEmpty(
          predicates, criteriaBuilder, filter.getQuery(), root.get("title"));

      // Simple equality filters
      if (filter.getLocationType() != null) {
        predicates.add(
            SpecificationUtils.createEqualPredicate(
                criteriaBuilder, root.get("locationType"), filter.getLocationType()));
      }

      if (filter.getEmploymentType() != null) {
        predicates.add(
            SpecificationUtils.createEqualPredicate(
                criteriaBuilder, root.get("employmentType"), filter.getEmploymentType()));
      }

      if (filter.getStatus() != null) {
        predicates.add(
            SpecificationUtils.createEqualPredicate(
                criteriaBuilder, root.get("status"), filter.getStatus()));
      }

      // Range filters
      SpecificationUtils.addGreaterThanOrEqualPredicateIfNotNull(
          predicates, root.get("yearsOfExperience"), filter.getMinExperience(), criteriaBuilder);

      SpecificationUtils.addLessThanOrEqualPredicateIfNotNull(
          predicates, root.get("yearsOfExperience"), filter.getMaxExperience(), criteriaBuilder);

      SpecificationUtils.addGreaterThanOrEqualPredicateIfNotNull(
          predicates, root.get("minSalary"), filter.getMinSalary(), criteriaBuilder);

      SpecificationUtils.addLessThanOrEqualPredicateIfNotNull(
          predicates, root.get("maxSalary"), filter.getMaxSalary(), criteriaBuilder);

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }
}
