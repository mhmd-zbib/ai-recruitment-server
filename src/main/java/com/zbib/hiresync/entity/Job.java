package com.zbib.hiresync.entity;

import com.zbib.hiresync.enums.EmploymentType;
import com.zbib.hiresync.enums.WorkplaceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.truncate;

@Data
@Entity
@Table(
    name = "jobs",
    indexes = {
        @Index(name = "idx_job_active", columnList = "active"),
        @Index(name = "idx_job_created_at", columnList = "created_at"),
        @Index(name = "idx_job_workplace_type", columnList = "workplace_type"),
        @Index(name = "idx_job_employment_type", columnList = "employment_type"),
        @Index(name = "idx_job_min_salary", columnList = "min_salary"),
        @Index(name = "idx_job_max_salary", columnList = "max_salary"),
        @Index(name = "idx_job_currency", columnList = "currency"),
        @Index(name = "idx_job_created_by_id", columnList = "created_by_id"),
        @Index(name = "idx_job_title_text", columnList = "title"),
        @Index(name = "idx_job_company_name", columnList = "company_name")
    }
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "requirements", nullable = false, columnDefinition = "TEXT")
    private String requirements;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "address_id")
    private Address address;

    @Enumerated(EnumType.STRING)
    @Column(name = "workplace_type", nullable = false)
    private WorkplaceType workplaceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false)
    private EmploymentType employmentType;

    @Column(name = "min_salary", columnDefinition = "DECIMAL(10,2)")
    private BigDecimal minSalary;

    @Column(name = "max_salary", columnDefinition = "DECIMAL(10,2)")
    private BigDecimal maxSalary;

    @Column(name = "currency", length = 3)
    private String currency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User user;

    @Column(name = "active", nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Application> applications = new ArrayList<>();

    @Column(name = "application_count", nullable = false)
    private int applicationCount = 0;

    public boolean isOwnedBy(User user) {
        return this.user != null && this.user.getId().equals(user.getId());
    }

    public boolean hasApplications() {
        return this.applicationCount > 0;
    }

    public void incrementApplicationCount() {
        this.applicationCount++;
    }

    @Override
    public String toString() {
        return "Job{" +
                "title='" + title + '\'' +
                ", companyName='" + companyName + '\'' +
                ", description='" + truncate(description, 200) + '\'' +
                ", requirements='" + truncate(requirements, 200) + '\'' +
                ", workplaceType=" + workplaceType +
                ", employmentType=" + employmentType +
                ", minSalary=" + minSalary +
                ", maxSalary=" + maxSalary +
                ", currency='" + currency + '\'' +
                '}';
    }
}