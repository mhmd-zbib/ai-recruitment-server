package com.zbib.hiresync.repository;

import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.JobPost;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
     * Find all applications for a specific job post
     *
     * @param jobPost the job post
     * @return list of applications for the job post
     */
    List<Application> findByJobPost(JobPost jobPost);

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
    
    /**
     * Find recent applications for job posts created by a specific user
     *
     * @param user the user who created the job posts
     * @param limit maximum number of applications to return
     * @return list of recent applications
     */
    @Query("SELECT a FROM Application a JOIN a.jobPost j WHERE j.createdBy = :user " +
           "ORDER BY a.createdAt DESC")
    List<Application> findRecentApplicationsForCreatedBy(@Param("user") User user, Pageable pageable);
    
    /**
     * Find recent applications for job posts created by a specific user with limit
     *
     * @param user the user who created the job posts
     * @param limit maximum number of applications to return
     * @return list of recent applications
     */
    @Query(value = "SELECT a.* FROM applications a " +
                   "JOIN job_posts j ON a.job_post_id = j.id " +
                   "WHERE j.created_by_id = :#{#user.id} " +
                   "ORDER BY a.created_at DESC LIMIT :limit", 
           nativeQuery = true)
    List<Application> findRecentApplicationsForCreatedBy(@Param("user") User user, @Param("limit") int limit);
    
    /**
     * Count applications for job posts created by a specific user
     *
     * @param user the user who created the job posts
     * @return the count of applications
     */
    @Query("SELECT COUNT(a) FROM Application a JOIN a.jobPost j WHERE j.createdBy = :user")
    long countByJobPostCreatedBy(@Param("user") User user);
    
    /**
     * Count applications by status for job posts created by a specific user
     *
     * @param user the user who created the job posts
     * @return array with status and count
     */
    @Query("SELECT a.status, COUNT(a) FROM Application a " +
           "JOIN a.jobPost j WHERE j.createdBy = :user " +
           "GROUP BY a.status")
    List<Object[]> countApplicationsByStatusForUser(@Param("user") User user);
    
    /**
     * Count applications by job post for a specific user
     *
     * @param user the user who created the job posts
     * @return array with job post ID and count
     */
    @Query("SELECT j.id, COUNT(a) FROM Application a " +
           "JOIN a.jobPost j WHERE j.createdBy = :user " +
           "GROUP BY j.id")
    List<Object[]> countApplicationsByJobPostForUser(@Param("user") User user);

    Page<Application> findByJobPostIdAndFilter(Specification<Application> applicationSpecification, Pageable pageable);
}