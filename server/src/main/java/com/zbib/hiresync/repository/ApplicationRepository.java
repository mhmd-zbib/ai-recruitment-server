package com.zbib.hiresync.repository;

import com.zbib.hiresync.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID>, JpaSpecificationExecutor<Application> {

    @Query("SELECT COUNT(a) > 0 FROM Application a " +
            "JOIN a.job j " +
            "WHERE a.id = :applicationId AND j.user.id = :userId")
    boolean existsByIdAndUserId(@Param("applicationId") UUID applicationId, @Param("userId") UUID userId);
}