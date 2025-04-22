package com.zbib.hiresync.repository;

import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.JobPost;
import com.zbib.hiresync.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for managing application data
 */
@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID>, JpaSpecificationExecutor<Application> {
    
    /**
     * Find all applications for a specific job post
     *
     * @param jobPost the job post
     * @param pageable pagination information
     * @return page of applications for the job post
     */
    Page<Application> findByJobPost(JobPost jobPost, Pageable pageable);

    /**
     * Find all applications by applicant email
     *
     * @param email the applicant's email
     * @param pageable pagination information
     * @return page of applications with matching email
     */
    Page<Application> findByApplicantEmail(String email, Pageable pageable);

    /**
     * Find all applications for a job post with a specific status
     *
     * @param jobPost the job post
     * @param status the application status
     * @param pageable pagination information
     * @return page of applications with matching job post and status
     */
    Page<Application> findByJobPostAndStatus(JobPost jobPost, ApplicationStatus status, Pageable pageable);

    /**
     * Count the number of applications for a job post
     *
     * @param jobPost the job post
     * @return the count of applications
     */
    long countByJobPost(JobPost jobPost);

    /**
     * Check if an email address has already applied to a job post
     *
     * @param jobPost the job post
     * @param email the applicant's email
     * @return true if the email has already applied
     */
    boolean existsByJobPostAndApplicantEmail(JobPost jobPost, String email);

    Page<Application> findByJobPostIdAndFilter(Specification<Application> applicationSpecification, Pageable pageable);
}