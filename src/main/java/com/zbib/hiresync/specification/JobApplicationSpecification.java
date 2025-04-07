package com.zbib.hiresync.specification;

import com.zbib.hiresync.dto.JobApplicationFilter;
import com.zbib.hiresync.entity.Application;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

/** Utility class for creating JobApplication specifications. */
@UtilityClass
public class JobApplicationSpecification {

  /**
   * Build a specification for Application entities based on the provided filter.
   *
   * @param jobId the job ID to filter by
   * @param filter the filter containing search criteria
   * @return a specification based on the filter
   */
  public static Specification<Application> buildSpecification(
      UUID jobId, JobApplicationFilter filter) {
    return (root, query, cb) -> {
      var predicates = new ArrayList<Predicate>();

      // Always filter by the job ID
      predicates.add(cb.equal(root.get("job").get("id"), jobId));

      if (filter != null) {
        // Free text search on multiple fields
        if (filter.getQuery() != null && !filter.getQuery().isEmpty()) {
          predicates.add(
              cb.or(
                  cb.like(
                      cb.lower(root.get("firstName")),
                      "%" + filter.getQuery().toLowerCase(Locale.ROOT) + "%"),
                  cb.like(
                      cb.lower(root.get("lastName")),
                      "%" + filter.getQuery().toLowerCase(Locale.ROOT) + "%"),
                  cb.like(
                      cb.lower(root.get("email")),
                      "%" + filter.getQuery().toLowerCase(Locale.ROOT) + "%")));
        }

        // Status filter
        if (filter.getStatus() != null) {
          predicates.add(cb.equal(root.get("status"), filter.getStatus()));
        }

        // Date range filters
        if (filter.getMinCreatedAt() != null) {
          predicates.add(cb.greaterThanOrEqualTo(root.get("appliedAt"), filter.getMinCreatedAt()));
        }

        if (filter.getMaxCreatedAt() != null) {
          predicates.add(cb.lessThanOrEqualTo(root.get("appliedAt"), filter.getMaxCreatedAt()));
        }
      }

      // Add ordering by createdAt desc by default
      query.orderBy(cb.desc(root.get("appliedAt")));

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}
