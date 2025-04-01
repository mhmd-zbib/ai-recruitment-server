package com.zbib.hiresync.specification;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

public class SpecificationUtils {

  public static Predicate createEqualPredicate(
      CriteriaBuilder criteriaBuilder, Path<?> path, Object value) {
    return criteriaBuilder.equal(path, value);
  }

  public static <T> void addPredicateIfNotEmpty(
      List<Predicate> predicates, Path<T> path, Collection<?> values) {
    if (!CollectionUtils.isEmpty(values)) {
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
    addGreaterThanOrEqualPredicateIfNotNull(predicates, path, value, criteriaBuilder);
  }

  public static void addDateLessThanOrEqualPredicateIfNotNull(
      List<Predicate> predicates,
      Path<LocalDateTime> path,
      LocalDateTime value,
      CriteriaBuilder criteriaBuilder) {
    addLessThanOrEqualPredicateIfNotNull(predicates, path, value, criteriaBuilder);
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
      String pattern = "%" + searchTerm.toLowerCase() + "%";
      Predicate[] searchPredicates = new Predicate[paths.length];
      for (int i = 0; i < paths.length; i++) {
        searchPredicates[i] = createLikePredicate(criteriaBuilder, paths[i], pattern);
      }
      predicates.add(criteriaBuilder.or(searchPredicates));
    }
  }
}
