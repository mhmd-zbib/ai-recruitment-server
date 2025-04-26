package com.zbib.hiresync.entity;

import com.zbib.hiresync.enums.ExperienceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a work or project experience in a CV
 */
@Data
@Entity
@Table(
    name = "experiences",
    indexes = {
        @Index(name = "idx_experience_cv_data_id", columnList = "cv_data_id"),
        @Index(name = "idx_experience_type", columnList = "type"),
        @Index(name = "idx_experience_start_date", columnList = "start_date"),
        @Index(name = "idx_experience_end_date", columnList = "end_date"),
        @Index(name = "idx_experience_company_name", columnList = "company_name")
    }
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Experience {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_data_id", nullable = false)
    private CVData cvData;

    @Column(name = "company_name", length = 100)
    private String companyName;

    @Column(name = "position", length = 100, nullable = false)
    private String position;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "current", nullable = false)
    private boolean current;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ExperienceType type;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "technologies_used", columnDefinition = "TEXT")
    private String technologiesUsed;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public int getDurationInMonths() {
        if (startDate == null) {
            return 0;
        }

        LocalDate endDateOrNow = endDate != null ? endDate : LocalDate.now();
        int years = endDateOrNow.getYear() - startDate.getYear();
        int months = endDateOrNow.getMonthValue() - startDate.getMonthValue();

        return years * 12 + months;
    }
    
    public boolean isRelevantForJob(Job job) {
        if (job == null || technologiesUsed == null) {
            return false;
        }
        
        String[] technologies = technologiesUsed.toLowerCase().split(",");
        Set<String> jobSkills = job.getSkillNames()
                .stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        
        for (String tech : technologies) {
            if (jobSkills.contains(tech.trim())) {
                return true;
            }
        }
        
        return false;
    }
}