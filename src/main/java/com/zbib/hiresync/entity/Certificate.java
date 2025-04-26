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
 * Represents a certificate from a CV
 */
@Data
@Entity
@Table(
    name = "certificates",
    indexes = {
        @Index(name = "idx_certificate_cv_data_id", columnList = "cv_data_id"),
        @Index(name = "idx_certificate_name", columnList = "name"),
        @Index(name = "idx_certificate_issuer", columnList = "issuer"),
        @Index(name = "idx_certificate_issue_date", columnList = "issue_date"),
        @Index(name = "idx_certificate_expiry_date", columnList = "expiry_date")
    }
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_data_id", nullable = false)
    private CVData cvData;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "issuer", length = 100, nullable = false)
    private String issuer;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "credential_id", length = 100)
    private String credentialId;

    @Column(name = "credential_url", length = 255)
    private String credentialUrl;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "has_expiry", nullable = false)
    private boolean hasExpiry;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean isExpired() {
        if (!hasExpiry || expiryDate == null) {
            return false;
        }
        return expiryDate.isBefore(LocalDate.now());
    }
    
    public boolean isRelevantForJob(Job job) {
        if (job == null || name == null) {
            return false;
        }
        
        String certificateName = name.toLowerCase();
        return job.getSkillNames().stream()
                .map(String::toLowerCase)
                .anyMatch(skill -> certificateName.contains(skill));
    }
}