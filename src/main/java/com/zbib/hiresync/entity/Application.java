package com.zbib.hiresync.entity;

import com.zbib.hiresync.enums.ApplicationStatus;
import com.zbib.hiresync.exception.application.InvalidApplicationStateException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

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
    
    public void removeSkill(Skill skill) {
        this.skills.remove(skill);
    }
    
    public boolean hasSkill(String skillName) {
        return skills.stream()
                .anyMatch(skill -> skill.getName().equalsIgnoreCase(skillName));
    }
    
    public Set<String> getSkillNames() {
        return skills.stream()
                .map(Skill::getName)
                .collect(Collectors.toSet());
    }
    
    public void validateCompleteness() {
        if (applicantName == null || applicantName.trim().isEmpty()) {
            throw InvalidApplicationStateException.missingName();
        }
        
        if (applicantEmail == null || applicantEmail.trim().isEmpty()) {
            throw InvalidApplicationStateException.missingEmail();
        }
        
        if (job == null) {
            throw InvalidApplicationStateException.missingJob();
        }
    }
    
    public boolean isActive() {
        // An application is considered active if it's not in a terminal state
        return !isInTerminalState();
    }
    
    public boolean isInTerminalState() {
        return status == ApplicationStatus.REJECTED || 
               status == ApplicationStatus.WITHDRAWN || 
               status == ApplicationStatus.OFFER_ACCEPTED;
    }
    
    public void updateStatus(ApplicationStatus newStatus, String statusChangeNotes) {
        validateStatusTransition(newStatus);
        
        this.status = newStatus;
        
        if (statusChangeNotes != null && !statusChangeNotes.trim().isEmpty()) {
            if (this.notes == null || this.notes.trim().isEmpty()) {
                this.notes = statusChangeNotes;
            } else {
                this.notes = this.notes + "\n\n" + LocalDateTime.now() + " - Status changed to " + 
                        newStatus + ": " + statusChangeNotes;
            }
        }
    }
    
    private void validateStatusTransition(ApplicationStatus newStatus) {
        if (this.status == newStatus) {
            return;
        }
        
        // Cannot change status if already in terminal state
        if (isInTerminalState()) {
            throw InvalidApplicationStateException.terminalState(this.status);
        }
        
        // Specific validation for transition to INTERVIEW_SCHEDULED
        if (newStatus == ApplicationStatus.INTERVIEW_SCHEDULED && this.status == ApplicationStatus.SUBMITTED) {
            // Validate that we have contact information
            if ((phoneNumber == null || phoneNumber.trim().isEmpty()) && 
                    (applicantEmail == null || applicantEmail.trim().isEmpty())) {
                throw InvalidApplicationStateException.missingContactInfo();
            }
        }
    }
    
    public boolean belongsToJob(UUID jobId) {
        return job != null && job.getId().equals(jobId);
    }
    
    public boolean belongsToRecruiter(User recruiter) {
        return job != null && 
               job.getCreatedBy() != null && 
               job.getCreatedBy().getId().equals(recruiter.getId());
    }
    
    public boolean canBeViewedBy(User user) {
        return belongsToRecruiter(user) || isApplicant(user.getEmail());
    }
    
    public boolean isApplicant(String email) {
        return applicantEmail.equalsIgnoreCase(email);
    }
    
    public double calculateMatchScore() {
        if (job == null || skills.isEmpty()) {
            return 0.0;
        }
        
        Set<String> jobSkills = job.getSkillNames();
        Set<String> applicantSkills = getSkillNames();
        
        if (jobSkills.isEmpty()) {
            return 0.0;
        }
        
        // Calculate intersection
        long matchCount = applicantSkills.stream()
                .filter(skill -> jobSkills.stream()
                        .anyMatch(jobSkill -> jobSkill.equalsIgnoreCase(skill)))
                .count();
        
        return (double) matchCount / jobSkills.size() * 100.0;
    }
} 