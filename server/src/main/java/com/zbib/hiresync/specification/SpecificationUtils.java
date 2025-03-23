package com.zbib.hiresync.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Utility class for creating common predicates used in specifications
 */
public class SpecificationUtils {

    /**
     * Creates an equal predicate for a given path and value
     *
     * @param criteriaBuilder The criteria builder
     * @param path The path to the attribute
     * @param value The value to compare with
     * @return The created predicate
     */
    public static Predicate createEqualPredicate(CriteriaBuilder criteriaBuilder, Path<?> path, Object value) {
        return criteriaBuilder.equal(path, value);
    }

    /**
     * Adds a predicate to the list if the collection is not empty
     *
     * @param predicates The list of predicates to add to
     * @param path The path to the attribute
     * @param values The collection of values
     */
    public static <T> void addPredicateIfNotEmpty(List<Predicate> predicates, Path<T> path, Collection<?> values) {
        if (!CollectionUtils.isEmpty(values)) {
            predicates.add(path.in(values));
        }
    }

    /**
     * Adds a greater than or equal predicate to the list if the value is not null
     *
     * @param predicates The list of predicates to add to
     * @param path The path to the attribute
     * @param value The value to compare with
     * @param criteriaBuilder The criteria builder
     */
    public static <T extends Comparable<? super T>> void addGreaterThanOrEqualPredicateIfNotNull(
            List<Predicate> predicates, Path<T> path, T value, CriteriaBuilder criteriaBuilder) {
        if (value != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(path, value));
        }
    }

    /**
     * Adds a less than or equal predicate to the list if the value is not null
     *
     * @param predicates The list of predicates to add to
     * @param path The path to the attribute
     * @param value The value to compare with
     * @param criteriaBuilder The criteria builder
     */
    public static <T extends Comparable<? super T>> void addLessThanOrEqualPredicateIfNotNull(
            List<Predicate> predicates, Path<T> path, T value, CriteriaBuilder criteriaBuilder) {
        if (value != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(path, value));
        }
    }

    /**
     * Adds a date greater than or equal predicate to the list if the value is not null
     *
     * @param predicates The list of predicates to add to
     * @param path The path to the attribute
     * @param value The value to compare with
     * @param criteriaBuilder The criteria builder
     */
    public static void addDateGreaterThanOrEqualPredicateIfNotNull(
            List<Predicate> predicates, Path<LocalDateTime> path, LocalDateTime value, CriteriaBuilder criteriaBuilder) {
        addGreaterThanOrEqualPredicateIfNotNull(predicates, path, value, criteriaBuilder);
    }

    /**
     * Adds a date less than or equal predicate to the list if the value is not null
     *
     * @param predicates The list of predicates to add to
     * @param path The path to the attribute
     * @param value The value to compare with
     * @param criteriaBuilder The criteria builder
     */
    public static void addDateLessThanOrEqualPredicateIfNotNull(
            List<Predicate> predicates, Path<LocalDateTime> path, LocalDateTime value, CriteriaBuilder criteriaBuilder) {
        addLessThanOrEqualPredicateIfNotNull(predicates, path, value, criteriaBuilder);
    }

    /**
     * Creates a like predicate for string search
     *
     * @param criteriaBuilder The criteria builder
     * @param path The path to the attribute
     * @param searchTerm The search term
     * @return The created predicate
     */
    public static Predicate createLikePredicate(CriteriaBuilder criteriaBuilder, Path<String> path, String searchTerm) {
        return criteriaBuilder.like(criteriaBuilder.lower(path), searchTerm);
    }

    /**
     * Adds a string search predicate to the list if the search term is not empty
     *
     * @param predicates The list of predicates to add to
     * @param criteriaBuilder The criteria builder
     * @param searchTerm The search term
     * @param paths The paths to search in
     */
    @SafeVarargs
    public static void addStringSearchPredicateIfNotEmpty(
            List<Predicate> predicates, CriteriaBuilder criteriaBuilder, String searchTerm, Path<String>... paths) {
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