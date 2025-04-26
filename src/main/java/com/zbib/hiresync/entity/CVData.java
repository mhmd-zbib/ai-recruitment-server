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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Represents parsed CV data from an application
 */
@Data
@Entity
@Table(
    name = "cv_data",
    indexes = {
        @Index(name = "idx_cv_data_application_id", columnList = "application_id"),
        @Index(name = "idx_cv_data_created_at", columnList = "created_at"),
        @Index(name = "idx_cv_data_updated_at", columnList = "updated_at")
    }
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CVData {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private Application application;

    @Column(name = "parsed_resume_text", columnDefinition = "TEXT")
    private String parsedResumeText;

    @ManyToMany
    @JoinTable(
        name = "cv_data_skills",
        joinColumns = @JoinColumn(name = "cv_data_id"),
        inverseJoinColumns = @JoinColumn(name = "skill_id"),
        indexes = {
            @Index(name = "idx_cv_data_skills_cv_data_id", columnList = "cv_data_id"),
            @Index(name = "idx_cv_data_skills_skill_id", columnList = "skill_id")
        }
    )
    @Builder.Default
    private Set<Skill> skills = new HashSet<>();

    @OneToMany(mappedBy = "cvData", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Experience> experiences = new ArrayList<>();

    @OneToMany(mappedBy = "cvData", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Education> educations = new ArrayList<>();

    @OneToMany(mappedBy = "cvData", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Certificate> certificates = new ArrayList<>();
    
    @Column(name = "ai_notes", columnDefinition = "TEXT")
    private String aiNotes;
    
    @Column(name = "overall_match_percentage")
    private Integer overallMatchPercentage;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void addSkill(Skill skill) {
        skills.add(skill);
    }

    public void removeSkill(Skill skill) {
        skills.remove(skill);
    }
    
    public void addExperience(Experience experience) {
        experience.setCvData(this);
        experiences.add(experience);
    }
    
    public void removeExperience(Experience experience) {
        experiences.remove(experience);
        experience.setCvData(null);
    }
    
    public void addEducation(Education education) {
        education.setCvData(this);
        educations.add(education);
    }
    
    public void removeEducation(Education education) {
        educations.remove(education);
        education.setCvData(null);
    }
    
    public void addCertificate(Certificate certificate) {
        certificate.setCvData(this);
        certificates.add(certificate);
    }
    
    public void removeCertificate(Certificate certificate) {
        certificates.remove(certificate);
        certificate.setCvData(null);
    }
    
    public Set<String> getSkillNames() {
        return skills.stream()
                .map(Skill::getName)
                .collect(Collectors.toSet());
    }
    
    public int getTotalYearsOfExperience() {
        return experiences.stream()
                .filter(exp -> exp.getEndDate() != null)
                .mapToInt(exp -> {
                    LocalDate start = exp.getStartDate();
                    LocalDate end = exp.getEndDate();
                    return end.getYear() - start.getYear();
                })
                .sum();
    }
}