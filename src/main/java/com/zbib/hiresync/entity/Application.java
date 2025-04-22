package com.zbib.hiresync.entity;

import com.zbib.hiresync.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a job application in the system
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_post_id", nullable = false)
    private JobPost jobPost;

    @Column(name = "applicant_name", nullable = false, length = 100)
    private String applicantName;

    @Column(name = "applicant_email", nullable = false, length = 100)
    private String applicantEmail;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;

    @Column(name = "resume_url", length = 255)
    private String resumeUrl;

    @Column(name = "portfolio_url", length = 255)
    private String portfolioUrl;

    @Column(name = "linkedin_url", length = 255)
    private String linkedinUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.SUBMITTED;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @ManyToMany
    @JoinTable(
        name = "application_skills",
        joinColumns = @JoinColumn(name = "application_id"),
        inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    @Builder.Default
    private Set<Skill> skills = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public void addSkill(Skill skill) {
        this.skills.add(skill);
    }
} 