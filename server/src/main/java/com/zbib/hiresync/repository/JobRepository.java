package com.zbib.hiresync.repository;

import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.enums.EmploymentType;
import com.zbib.hiresync.enums.JobStatus;
import com.zbib.hiresync.enums.LocationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID>, JpaSpecificationExecutor<Job> {
    
    Page<Job> findByStatus(JobStatus status, Pageable pageable);
    
    Page<Job> findByDepartment(String department, Pageable pageable);
    
    Page<Job> findByLocationType(LocationType locationType, Pageable pageable);
    
    Page<Job> findByEmploymentType(EmploymentType employmentType, Pageable pageable);
    
    Page<Job> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);
    
    Page<Job> findByYearsOfExperienceLessThanEqual(int yearsOfExperience, Pageable pageable);
    
    Page<Job> findByMinSalaryGreaterThanEqualAndMaxSalaryLessThanEqual(int minSalary, int maxSalary, Pageable pageable);
    
    Page<Job> findByUserId(Long userId, Pageable pageable);
}