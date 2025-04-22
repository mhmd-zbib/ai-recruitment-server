package com.zbib.hiresync.repository;

import com.zbib.hiresync.entity.JobPost;
import com.zbib.hiresync.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for managing job post data
 */
@Repository
public interface JobPostRepository extends JpaRepository<JobPost, UUID>, JpaSpecificationExecutor<JobPost> {

    /**
     * Find job posts by creator
     *
     * @param createdBy the user who created the job posts
     * @param pageable pagination information
     * @return page of job posts created by the user
     */
    Page<JobPost> findByCreatedBy(User createdBy, Pageable pageable);
    
    /**
     * Find active job posts
     *
     * @param pageable pagination information
     * @return page of active job posts
     */
    @Query("SELECT j FROM JobPost j WHERE j.active = true AND " +
           "(j.visibleUntil IS NULL OR j.visibleUntil > CURRENT_TIMESTAMP)")
    Page<JobPost> findActiveJobPosts(Pageable pageable);
    
    /**
     * Find job posts expiring soon for a specific user
     *
     * @param user the user who created the job posts
     * @param thresholdDate the date before which job posts are considered expiring soon
     * @param active whether to include only active job posts
     * @return list of job posts expiring soon
     */
    @Query("SELECT j FROM JobPost j WHERE j.createdBy = :user AND " +
           "j.visibleUntil < :thresholdDate AND j.active = :active")
    List<JobPost> findByCreatedByAndVisibleUntilBeforeAndActive(
            @Param("user") User user, 
            @Param("thresholdDate") LocalDateTime thresholdDate,
            @Param("active") boolean active);
    
    /**
     * Count active job posts for a specific user
     *
     * @param user the user who created the job posts
     * @return count of active job posts
     */
    @Query("SELECT COUNT(j) FROM JobPost j WHERE j.createdBy = :user AND j.active = true AND " +
           "(j.visibleUntil IS NULL OR j.visibleUntil > CURRENT_TIMESTAMP)")
    long countActiveJobPostsByUser(@Param("user") User user);
    
    /**
     * Count job posts by active status
     *
     * @param user the user who created the job posts
     * @return array with active status and count
     */
    @Query("SELECT j.active, COUNT(j) FROM JobPost j " +
           "WHERE j.createdBy = :user GROUP BY j.active")
    List<Object[]> countJobPostsByActiveStatus(@Param("user") User user);
} 