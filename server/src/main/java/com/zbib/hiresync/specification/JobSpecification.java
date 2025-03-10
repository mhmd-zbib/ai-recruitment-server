package com.zbib.hiresync.specification;

import com.zbib.hiresync.dto.JobFilter;
import com.zbib.hiresync.entity.Job;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Specification class for Job entity filtering
 */
public class JobSpecification {

    /**
     * Creates a specification for filtering jobs based on various criteria
     *
     * @param userId the ID of the user who created the job
     * @param department the department of the job
     * @param locationType the location type of the job
     * @param employmentType the employment type of the job
     * @param status the status of the job
     * @param keyword keyword to search in job title
     * @param minExperience minimum years of experience required
     * @param maxExperience maximum years of experience required
     * @param minSalary minimum salary offered
     * @param maxSalary maximum salary offered
     * @return a specification for filtering jobs
     */
    /**
     * Creates a specification for filtering jobs based on JobFilter criteria
     *
     * @param filter the JobFilter containing all filter criteria
     * @return a specification for filtering jobs
     */
    public static Specification<Job> filterJobs(JobFilter filter) {
        
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (filter.getUserId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("user").get("id"), filter.getUserId()));
            }
            
            if (filter.getDepartment() != null && !filter.getDepartment().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("department"), filter.getDepartment()));
            }
            
            if (filter.getLocationType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("locationType"), filter.getLocationType()));
            }
            
            if (filter.getEmploymentType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("employmentType"), filter.getEmploymentType()));
            }
            
            if (filter.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
            }
            
            if (filter.getKeyword() != null && !filter.getKeyword().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("title")), 
                        "%" + filter.getKeyword().toLowerCase() + "%"));
            }
            
            if (filter.getMinExperience() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("yearsOfExperience"), filter.getMinExperience()));
            }
            
            if (filter.getMaxExperience() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("yearsOfExperience"), filter.getMaxExperience()));
            }
            
            if (filter.getMinSalary() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("minSalary"), filter.getMinSalary()));
            }
            
            if (filter.getMaxSalary() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("maxSalary"), filter.getMaxSalary()));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}