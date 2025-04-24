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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Represents a job in the system
 */
@Data
@Entity
@Table(
    name = "jobs",
    indexes = {
        @Index(name = "idx_job_active", columnList = "active"),
        @Index(name = "idx_job_visible_until", columnList = "visible_until"),
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
    private User createdBy;

    @ManyToMany
    @JoinTable(
        name = "job_skills",
        joinColumns = @JoinColumn(name = "job_id"),
        inverseJoinColumns = @JoinColumn(name = "skill_id"),
        indexes = {
            @Index(name = "idx_job_skills_job_id", columnList = "job_id"),
            @Index(name = "idx_job_skills_skill_id", columnList = "skill_id")
        }
    )
    @Builder.Default
    private Set<Skill> skills = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "job_tags",
        joinColumns = @JoinColumn(name = "job_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id"),
        indexes = {
            @Index(name = "idx_job_tags_job_id", columnList = "job_id"),
            @Index(name = "idx_job_tags_tag_id", columnList = "tag_id")
        }
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "visible_until")
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime visibleUntil;

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

    public boolean isActive() {
        return this.active && !isExpired();
    }

    public boolean isExpired() {
        return this.visibleUntil != null && this.visibleUntil.isBefore(LocalDateTime.now());
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean isOwnedBy(User user) {
        return this.createdBy != null && this.createdBy.getId().equals(user.getId());
    }

    public boolean hasApplications() {
        return this.applicationCount > 0;
    }

    public void incrementApplicationCount() {
        this.applicationCount++;
    }

    public void extendVisibilityBy(int days) {
        LocalDateTime newDate;
        if (this.visibleUntil == null || this.visibleUntil.isBefore(LocalDateTime.now())) {
            newDate = LocalDateTime.now().plusDays(days);
        } else {
            newDate = this.visibleUntil.plusDays(days);
        }
        this.visibleUntil = newDate;
    }

    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    public Job addSkill(Skill skill) {
        skills.add(skill);
        return this;
    }

    public Job removeSkill(Skill skill) {
        skills.remove(skill);
        return this;
    }

    public Job addTag(Tag tag) {
        tags.add(tag);
        return this;
    }

    public Job removeTag(Tag tag) {
        tags.remove(tag);
        return this;
    }

    public Set<String> getSkillNames() {
        return skills.stream()
                .map(Skill::getName)
                .collect(Collectors.toSet());
    }
} 