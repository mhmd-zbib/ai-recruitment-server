package com.zbib.hiresync.repository;

import com.zbib.hiresync.entity.Job;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID>, JpaSpecificationExecutor<Job> {
  boolean existsByIdAndUserId(UUID jobId, UUID userId);
}
