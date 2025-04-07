package com.zbib.hiresync.specification;

import com.zbib.hiresync.dto.ApplicationFilter;
import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.JobPosting;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

/** Utility class for creating Application specifications. */
@UtilityClass
public class ApplicationSpecification {

  /**
   * Build a specification for Application entities based on the provided filter.
   *
   * @param userId the user ID for filtering
   * @param filter the filter containing search criteria
   * @return a specification based on the filter
   */
  public static Specification<Application> buildSpecification(
      UUID userId, ApplicationFilter filter) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      Join<Application, JobPosting> jobJoin = root.join("job", JoinType.INNER);

      if (userId != null) {
        predicates.add(cb.equal(root.get("user").get("id"), userId));
      }

      if (filter.getJobId() != null && !filter.getJobId().isEmpty()) {
        predicates.add(jobJoin.get("id").in(filter.getJobId()));
      }

      if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
        predicates.add(root.get("status").in(filter.getStatus()));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}
