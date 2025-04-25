package com.zbib.hiresync.repository;

import com.zbib.hiresync.dto.JobCountDTO;
import com.zbib.hiresync.dto.StatusCountDTO;
import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.Job;
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
     * Find all applications for a specific job
     *
     * @param job the job
     * @param pageable pagination information
     * @return page of applications for the job
     */
    Page<Application> findByJob(Job job, Pageable pageable);
    
    /**
     * Find all applications for a specific job
     *
     * @param job the job
     * @return list of applications for the job
     */
    List<Application> findByJob(Job job);

    /**
     * Find all applications by applicant email
     *
     * @param email the applicant's email
     * @param pageable pagination information
     * @return page of applications with matching email
     */
    Page<Application> findByApplicantEmail(String email, Pageable pageable);

    /**
     * Find all applications for a job with a specific status
     *
     * @param job the job
     * @param status the application status
     * @param pageable pagination information
     * @return page of applications with matching job and status
     */
    Page<Application> findByJobAndStatus(Job job, ApplicationStatus status, Pageable pageable);

    /**
     * Count the number of applications for a job
     *
     * @param job the job
     * @return the count of applications
     */
    long countByJob(Job job);

    /**
     * Check if an email address has already applied to a job
     *
     * @param job the job
     * @param email the applicant's email
     * @return true if the email has already applied
     */
    boolean existsByJobAndApplicantEmail(Job job, String email);
    
    /**
     * Find recent applications for jobs created by a specific user
     *
     * @param user the user who created the jobs
     * @param limit maximum number of applications to return
     * @return list of recent applications
     */
    @Query("SELECT a FROM Application a JOIN a.job j WHERE j.createdBy = :user " +
           "ORDER BY a.createdAt DESC")
    List<Application> findRecentApplicationsForCreatedBy(@Param("user") User user, Pageable pageable);
    
    /**
     * Find recent applications for jobs created by a specific user with limit
     *
     * @param user the user who created the jobs
     * @param limit maximum number of applications to return
     * @return list of recent applications
     */
    @Query(value = "SELECT a.* FROM applications a " +
                   "JOIN jobs j ON a.job_id = j.id " +
                   "WHERE j.created_by_id = :#{#user.id} " +
                   "ORDER BY a.created_at DESC LIMIT :limit", 
           nativeQuery = true)
    List<Application> findRecentApplicationsForCreatedBy(@Param("user") User user, @Param("limit") int limit);
    
    /**
     * Count applications for jobs created by a specific user
     *
     * @param user the user who created the jobs
     * @return the count of applications
     */
    @Query("SELECT COUNT(a) FROM Application a JOIN a.job j WHERE j.createdBy = :user")
    long countByJobPostCreatedBy(@Param("user") User user);
    
    /**
     * Count applications by status for jobs created by a specific user
     *
     * @param user the user who created the jobs
     * @return list of status count DTOs
     */
    @Query("SELECT new com.zbib.hiresync.dto.StatusCountDTO(a.status, COUNT(a)) " +
           "FROM Application a " +
           "JOIN a.job j WHERE j.createdBy = :user " +
           "GROUP BY a.status")
    List<StatusCountDTO> countApplicationsByStatusForUser(@Param("user") User user);
    
    /**
     * Count applications by job for a specific user
     *
     * @param user the user who created the jobs
     * @return list of job count DTOs
     */
    @Query("SELECT new com.zbib.hiresync.dto.JobCountDTO(j.id, COUNT(a)) " +
           "FROM Application a " +
           "JOIN a.job j WHERE j.createdBy = :user " +
           "GROUP BY j.id")
    List<JobCountDTO> countApplicationsByJobPostForUser(@Param("user") User user);

    /**
     * Find applications with the given job ID using a specification for filtering
     * 
     * @param jobId the job ID
     * @param specification the specification to filter results
     * @param pageable pagination information
     * @return page of applications matching the criteria
     */
    @Query("SELECT a FROM Application a WHERE a.job.id = :jobId")
    Page<Application> findByJobId(@Param("jobId") UUID jobId, Pageable pageable);
    
    /**
     * Find applications using a specification for filtering
     * 
     * @param specification the specification to filter results
     * @param pageable pagination information
     * @return page of applications matching the criteria
     */
    Page<Application> findAll(Specification<Application> specification, Pageable pageable);
}