package com.zbib.hiresync.dto;

import com.zbib.hiresync.enums.EmploymentType;
import com.zbib.hiresync.enums.JobStatus;
import com.zbib.hiresync.enums.LocationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobFilter {

    private String department;

    private LocationType locationType;

    private EmploymentType employmentType;

    private JobStatus status;

    private String keyword;

    private Integer minExperience;

    private Integer maxExperience;

    private Integer minSalary;

    private Integer maxSalary;
}