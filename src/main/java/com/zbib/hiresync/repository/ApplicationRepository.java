package com.zbib.hiresync.repository;

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

    boolean existsByJobAndApplicantEmail(Job job, String email);

    @Query("SELECT a FROM Application a JOIN a.job j WHERE j.createdBy = :user " +
           "ORDER BY a.createdAt DESC")
    List<Application> findRecentApplicationsForCreatedBy(@Param("user") User user, Pageable pageable);

    @Query(value = "SELECT a.* FROM applications a " +
                   "JOIN jobs j ON a.job_id = j.id " +
                   "WHERE j.created_by_id = :#{#user.id} " +
                   "ORDER BY a.created_at DESC LIMIT :limit",
           nativeQuery = true)
    List<Application> findRecentApplicationsForCreatedBy(@Param("user") User user, @Param("limit") int limit);


    @Query("SELECT COUNT(a) FROM Application a JOIN a.job j WHERE j.createdBy = :user")
    long countByJobPostCreatedBy(@Param("user") User user);

//    @Query("SELECT new com.zbib.hiresync.dto.StatusCountDTO(a.status, COUNT(a)) " +
//           "FROM Application a " +
//           "JOIN a.job j WHERE j.createdBy = :user " +
//           "GROUP BY a.status")
//    List<StatusCountDTO> countApplicationsByStatusForUser(@Param("user") User user);
//

//    @Query("SELECT new com.zbib.hiresync.dto.JobCountDTO(j.id, COUNT(a)) " +
//           "FROM Application a " +
//           "JOIN a.job j WHERE j.createdBy = :user " +
//           "GROUP BY j.id")
//    List<JobCountDTO> countApplicationsByJobPostForUser(@Param("user") User user);


    Page<Application> findAll(Specification<Application> specification, Pageable pageable);
}