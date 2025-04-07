package com.zbib.hiresync.specification;

import com.zbib.hiresync.dto.JobFilter;
import com.zbib.hiresync.entity.JobPosting;
import com.zbib.hiresync.enums.EmploymentType;
import com.zbib.hiresync.enums.JobStatus;
import com.zbib.hiresync.enums.LocationType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

/** Utility class for creating Job specifications. */
@UtilityClass
public class JobSpecification {

  /**
   * Build a specification for JobPosting entities based on the provided filter.
   *
   * @param userId user ID to filter by
   * @param filter the filter containing search criteria
   * @return a specification based on the filter
   */
  public static Specification<JobPosting> buildSpecification(UUID userId, JobFilter filter) {
    return (root, query, cb) -> {
      var predicates = new ArrayList<Predicate>();

      if (userId != null) {
        predicates.add(withUserId(userId).toPredicate(root, query, cb));
      }

      if (filter != null) {
        if (filter.getQuery() != null) {
          predicates.add(withTitle(filter.getQuery()).toPredicate(root, query, cb));
        }
        if (filter.getLocationType() != null) {
          predicates.add(withLocationType(filter.getLocationType()).toPredicate(root, query, cb));
        }
        if (filter.getEmploymentType() != null) {
          predicates.add(
              withEmploymentType(filter.getEmploymentType()).toPredicate(root, query, cb));
        }
        if (filter.getStatus() != null) {
          predicates.add(withStatus(filter.getStatus()).toPredicate(root, query, cb));
        }
        if (filter.getMinExperience() != null) {
          predicates.add(withMinExperience(filter.getMinExperience()).toPredicate(root, query, cb));
        }
        if (filter.getMinSalary() != null) {
          predicates.add(withMinSalary(filter.getMinSalary()).toPredicate(root, query, cb));
        }
        if (filter.getMaxSalary() != null) {
          predicates.add(withMaxSalary(filter.getMaxSalary()).toPredicate(root, query, cb));
        }
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  private static Specification<JobPosting> withUserId(UUID userId) {
    return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
  }

  private static Specification<JobPosting> withTitle(String title) {
    return (root, query, cb) ->
        cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase(Locale.ROOT) + "%");
  }

  private static Specification<JobPosting> withLocationType(LocationType locationType) {
    return (root, query, cb) -> cb.equal(root.get("locationType"), locationType);
  }

  private static Specification<JobPosting> withEmploymentType(EmploymentType employmentType) {
    return (root, query, cb) -> cb.equal(root.get("employmentType"), employmentType);
  }

  private static Specification<JobPosting> withStatus(JobStatus status) {
    return (root, query, cb) -> cb.equal(root.get("status"), status);
  }

  private static Specification<JobPosting> withMinExperience(Integer minExperience) {
    return (root, query, cb) ->
        cb.greaterThanOrEqualTo(root.get("yearsOfExperience"), minExperience);
  }

  private static Specification<JobPosting> withMinSalary(Integer minSalary) {
    return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("minSalary"), minSalary);
  }

  private static Specification<JobPosting> withMaxSalary(Integer maxSalary) {
    return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("maxSalary"), maxSalary);
  }
}
