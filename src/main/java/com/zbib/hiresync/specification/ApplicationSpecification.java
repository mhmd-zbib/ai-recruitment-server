package com.zbib.hiresync.specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.zbib.hiresync.dto.ApplicationFilter;
import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.JobPosting;
import com.zbib.hiresync.enums.ApplicationStatus;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

public final class ApplicationSpecification {

  private ApplicationSpecification() {
    throw new IllegalStateException("Utility class");
  }

  public static Specification<Application> buildSpecification(UUID userId, ApplicationFilter filter) {
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
