package com.zbib.hiresync.specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.zbib.hiresync.dto.JobFilter;
import com.zbib.hiresync.entity.JobPosting;

import jakarta.persistence.criteria.Predicate;

public class JobSpecification {

  public static Specification<JobPosting> buildSpecification(UUID userId, JobFilter filter) {
    return Specification.where(hasUserId(userId))
            .and(hasTitle(filter.getQuery()))
            .and(hasLocationType(filter.getLocationType()))
            .and(hasEmploymentType(filter.getEmploymentType()))
            .and(hasStatus(filter.getStatus()))
            .and(hasMinExperience(filter.getMinExperience(), filter.getMaxExperience()))
            .and(hasMinSalary(filter.getMinSalary()))
            .and(hasMaxSalary(filter.getMaxSalary()));
  }

  private static Specification<JobPosting> hasUserId(UUID userId) {
    return (root, query, cb) -> userId == null ? null : cb.equal(root.get("user").get("id"), userId);
  }

  private static Specification<JobPosting> hasTitle(String title) {
    return (root, query, cb) -> title == null ? null : cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%");
  }

  private static Specification<JobPosting> hasLocationType(String locationType) {
    return (root, query, cb) -> locationType == null ? null : cb.equal(root.get("locationType"), locationType);
  }

  private static Specification<JobPosting> hasEmploymentType(String employmentType) {
    return (root, query, cb) -> employmentType == null ? null : cb.equal(root.get("employmentType"), employmentType);
  }

  private static Specification<JobPosting> hasStatus(String status) {
    return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
  }

  private static Specification<JobPosting> hasMinExperience(Integer minExperience, Integer maxExperience) {
    return (root, query, cb) -> {
      if (minExperience == null && maxExperience == null) {
        return null;
      }
      if (minExperience == null) {
        return cb.lessThanOrEqualTo(root.get("yearsOfExperience"), maxExperience);
      }
      if (maxExperience == null) {
        return cb.greaterThanOrEqualTo(root.get("yearsOfExperience"), minExperience);
      }
      return cb.between(root.get("yearsOfExperience"), minExperience, maxExperience);
    };
  }

  private static Specification<JobPosting> hasMinSalary(Integer minSalary) {
    return (root, query, cb) -> minSalary == null ? null : cb.greaterThanOrEqualTo(root.get("minSalary"), minSalary);
  }

  private static Specification<JobPosting> hasMaxSalary(Integer maxSalary) {
    return (root, query, cb) -> maxSalary == null ? null : cb.lessThanOrEqualTo(root.get("maxSalary"), maxSalary);
  }
}
