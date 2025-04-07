package com.zbib.hiresync.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/** Utility class for creating JPA Specification predicates. */
@UtilityClass
public class SpecificationUtils {

  public static Predicate createEqualPredicate(
      CriteriaBuilder criteriaBuilder, Path<?> path, Object value) {
    return criteriaBuilder.equal(path, value);
  }

  public static <T> void addPredicateIfNotEmpty(
      List<Predicate> predicates, Path<T> path, Collection<?> values) {
    if (values != null && !values.isEmpty()) {
      predicates.add(path.in(values));
    }
  }

  public static <T extends Comparable<? super T>> void addGreaterThanOrEqualPredicateIfNotNull(
      List<Predicate> predicates, Path<T> path, T value, CriteriaBuilder criteriaBuilder) {
    if (value != null) {
      predicates.add(criteriaBuilder.greaterThanOrEqualTo(path, value));
    }
  }

  public static <T extends Comparable<? super T>> void addLessThanOrEqualPredicateIfNotNull(
      List<Predicate> predicates, Path<T> path, T value, CriteriaBuilder criteriaBuilder) {
    if (value != null) {
      predicates.add(criteriaBuilder.lessThanOrEqualTo(path, value));
    }
  }

  public static void addDateGreaterThanOrEqualPredicateIfNotNull(
      List<Predicate> predicates,
      Path<LocalDateTime> path,
      LocalDateTime value,
      CriteriaBuilder criteriaBuilder) {
    if (value != null) {
      predicates.add(criteriaBuilder.greaterThanOrEqualTo(path, value));
    }
  }

  public static void addDateLessThanOrEqualPredicateIfNotNull(
      List<Predicate> predicates,
      Path<LocalDateTime> path,
      LocalDateTime value,
      CriteriaBuilder criteriaBuilder) {
    if (value != null) {
      predicates.add(criteriaBuilder.lessThanOrEqualTo(path, value));
    }
  }

  public static Predicate createLikePredicate(
      CriteriaBuilder criteriaBuilder, Path<String> path, String searchTerm) {
    return criteriaBuilder.like(criteriaBuilder.lower(path), searchTerm);
  }

  @SafeVarargs
  public static void addStringSearchPredicateIfNotEmpty(
      List<Predicate> predicates,
      CriteriaBuilder criteriaBuilder,
      String searchTerm,
      Path<String>... paths) {
    if (StringUtils.hasText(searchTerm)) {
      String pattern = "%" + searchTerm.toLowerCase(Locale.ROOT) + "%";
      Predicate[] searchPredicates = new Predicate[paths.length];
      for (int i = 0; i < paths.length; i++) {
        searchPredicates[i] = createLikePredicate(criteriaBuilder, paths[i], pattern);
      }
      predicates.add(criteriaBuilder.or(searchPredicates));
    }
  }

  public static <T> Specification<T> like(String field, String value) {
    return (root, query, cb) -> {
      if (value == null) {
        return null;
      }
      return cb.like(cb.lower(root.get(field)), "%" + value.toLowerCase(Locale.ROOT) + "%");
    };
  }

  public static <T> Specification<T> equalTo(String field, Object value) {
    return (root, query, cb) -> {
      if (value == null) {
        return null;
      }
      return cb.equal(root.get(field), value);
    };
  }

  public static <T> Specification<T> startsWith(String field, String value) {
    return (root, query, cb) -> {
      if (value == null) {
        return null;
      }
      return cb.like(cb.lower(root.get(field)), value.toLowerCase(Locale.ROOT) + "%");
    };
  }
}
