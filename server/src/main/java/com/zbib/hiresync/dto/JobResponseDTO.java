package com.zbib.hiresync.dto;

import com.zbib.hiresync.enums.EmploymentType;
import com.zbib.hiresync.enums.JobStatus;
import com.zbib.hiresync.enums.LocationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobResponseDTO {
    
    private UUID id;
    
    private Long userId;
    
    private String username;
    
    private String title;
    
    private String department;
    
    private String description;
    
    private String responsibilities;
    
    private String qualifications;
    
    private String benefits;
    
    private int yearsOfExperience;
    
    private LocationType locationType;
    
    private EmploymentType employmentType;
    
    private JobStatus status;
    
    private int minSalary;
    
    private int maxSalary;
    
    private LocalDateTime createdAt;
}