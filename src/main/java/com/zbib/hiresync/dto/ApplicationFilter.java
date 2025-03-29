package com.zbib.hiresync.dto;

import com.zbib.hiresync.enums.ApplicationStatus;
import com.zbib.hiresync.enums.EmploymentType;
import com.zbib.hiresync.enums.JobStatus;
import com.zbib.hiresync.enums.LocationType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ApplicationFilter {

    // General search query
    private String query;

    // Job-related filters
    private List<Long> jobId;
    private List<LocationType> locationType;
    private List<EmploymentType> employmentType;
    private List<JobStatus> jobStatus;

    // Experience and salary ranges
    private Integer minExperience;
    private Integer maxExperience;
    private Integer minSalary;
    private Integer maxSalary;

    // Application status (multiple values)
    private List<ApplicationStatus> status;

    // Date filters
    private LocalDateTime minJobCreatedAt;
    private LocalDateTime maxJobCreatedAt;
    private LocalDateTime minCreatedAt;
    private LocalDateTime maxCreatedAt;

}