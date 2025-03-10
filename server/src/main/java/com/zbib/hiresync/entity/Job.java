package com.zbib.hiresync.entity;

import com.zbib.hiresync.enums.EmploymentType;
import com.zbib.hiresync.enums.JobStatus;
import com.zbib.hiresync.enums.LocationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    private User user;

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
