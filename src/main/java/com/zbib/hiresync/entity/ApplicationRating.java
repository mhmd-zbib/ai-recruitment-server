package com.zbib.hiresync.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents AI-generated rating and feedback for a job application
 */
@Data
@Entity
@Table(
    name = "application_ratings",
    indexes = {
        @Index(name = "idx_application_rating_application_id", columnList = "application_id"),
        @Index(name = "idx_application_rating_job_id", columnList = "job_id"),
        @Index(name = "idx_application_rating_overall_score", columnList = "overall_score"),
        @Index(name = "idx_application_rating_created_at", columnList = "created_at")
    }
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationRating {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private Application application;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "overall_score", nullable = false)
    private Integer overallScore;
    
    @Column(name = "skills_match_score")
    private Integer skillsMatchScore;
    
    @Column(name = "experience_match_score")
    private Integer experienceMatchScore;
    
    @Column(name = "education_match_score")
    private Integer educationMatchScore;
    
    @Column(name = "certificates_match_score")
    private Integer certificatesMatchScore;

    @Column(name = "summary", length = 500)
    private String summary;
    
    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths;
    
    @Column(name = "weaknesses", columnDefinition = "TEXT")
    private String weaknesses;
    
    @Column(name = "interview_recommendations", columnDefinition = "TEXT")
    private String interviewRecommendations;
    
    @Column(name = "suggested_questions", columnDefinition = "TEXT")
    private String suggestedQuestions;
    
    @Column(name = "general_notes", columnDefinition = "TEXT")
    private String generalNotes;
    
    @Column(name = "talent_fit_category", length = 50)
    private String talentFitCategory; // e.g., "Strong Match", "Moderate Match", "Weak Match"

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public boolean isStrongMatch() {
        return overallScore >= 80;
    }
    
    public boolean isModerateMatch() {
        return overallScore >= 60 && overallScore < 80;
    }
    
    public boolean isWeakMatch() {
        return overallScore < 60;
    }
    
    public String getFormattedOverallScore() {
        return overallScore + "%";
    }
    
    /**
     * Calculate a weighted average of all component scores
     * This can be used to recalculate the overall score if individual components are updated
     */
    public void recalculateOverallScore() {
        int skillWeight = 40;
        int experienceWeight = 30;
        int educationWeight = 20;
        int certificatesWeight = 10;
        
        int weightedSum = 0;
        int totalWeight = 0;
        
        if (skillsMatchScore != null) {
            weightedSum += skillsMatchScore * skillWeight;
            totalWeight += skillWeight;
        }
        
        if (experienceMatchScore != null) {
            weightedSum += experienceMatchScore * experienceWeight;
            totalWeight += experienceWeight;
        }
        
        if (educationMatchScore != null) {
            weightedSum += educationMatchScore * educationWeight;
            totalWeight += educationWeight;
        }
        
        if (certificatesMatchScore != null) {
            weightedSum += certificatesMatchScore * certificatesWeight;
            totalWeight += certificatesWeight;
        }
        
        if (totalWeight > 0) {
            this.overallScore = weightedSum / totalWeight;
            
            // Determine the talent fit category based on the overall score
            if (isStrongMatch()) {
                this.talentFitCategory = "Strong Match";
            } else if (isModerateMatch()) {
                this.talentFitCategory = "Moderate Match";
            } else {
                this.talentFitCategory = "Weak Match";
            }
        }
    }
}