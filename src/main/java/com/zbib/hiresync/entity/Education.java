package com.zbib.hiresync.entity;

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
 * Represents an education entry in a CV
 */
@Data
@Entity
@Table(
    name = "educations",
    indexes = {
        @Index(name = "idx_education_cv_data_id", columnList = "cv_data_id"),
        @Index(name = "idx_education_school_name", columnList = "school_name"),
        @Index(name = "idx_education_degree", columnList = "degree"),
        @Index(name = "idx_education_field_of_study", columnList = "field_of_study"),
        @Index(name = "idx_education_start_date", columnList = "start_date"),
        @Index(name = "idx_education_end_date", columnList = "end_date")
    }
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Education {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_data_id", nullable = false)
    private CVData cvData;

    @Column(name = "school_name", length = 100, nullable = false)
    private String schoolName;

    @Column(name = "degree", length = 100, nullable = false)
    private String degree;

    @Column(name = "field_of_study", length = 100)
    private String fieldOfStudy;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "current", nullable = false)
    private boolean current;

    @Column(name = "gpa", length = 10)
    private String gpa;

    @Column(name = "location", length = 100)
    private String location;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean isCompleted() {
        return endDate != null && !current;
    }
    
    public String getFormattedDuration() {
        if (startDate == null) {
            return "";
        }
        
        String endStr = current ? "Present" : (endDate != null ? endDate.getYear() + "" : "");
        return startDate.getYear() + " - " + endStr;
    }
}