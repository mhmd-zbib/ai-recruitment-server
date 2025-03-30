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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 50)
    private String department;

    @Column(nullable = false, length = 2000, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 2000, columnDefinition = "TEXT")
    private String responsibilities;

    @Column(nullable = false, length = 2000, columnDefinition = "TEXT")
    private String qualifications;

    @Column(length = 1000, columnDefinition = "TEXT")
    private String benefits;

    @Column(nullable = false)
    private int yearsOfExperience;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LocationType locationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    private int minSalary;

    private int maxSalary;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}