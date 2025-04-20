package com.zbib.hiresync.repository;

import com.zbib.hiresync.entity.JobPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for job posts with both basic CRUD operations and
 * support for specifications-based querying
 */
@Repository
public interface JobPostRepository extends JpaRepository<JobPost, UUID>, JpaSpecificationExecutor<JobPost> {
    // The repository now uses JpaSpecificationExecutor to allow flexible querying with specifications
    // No need for custom query methods as they will be implemented using specifications
    
    Page<JobPost> findByCreatedById(UUID createdById, Pageable pageable);
    
    Page<JobPost> findByActiveTrue(Pageable pageable);
} 