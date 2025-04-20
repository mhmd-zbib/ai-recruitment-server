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
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a job posting in the system
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "job_posts")
public class JobPost {

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

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
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
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "job_post_skills",
        joinColumns = @JoinColumn(name = "job_post_id"),
        inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    @Builder.Default
    private Set<Skill> skills = new HashSet<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "job_post_tags",
        joinColumns = @JoinColumn(name = "job_post_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
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

    /**
     * Adds a skill to the job post
     * 
     * @param skill the skill to add
     * @return this job post for chaining
     */
    public JobPost addSkill(Skill skill) {
        skills.add(skill);
        skill.getJobPosts().add(this);
        return this;
    }

    /**
     * Removes a skill from the job post
     * 
     * @param skill the skill to remove
     * @return this job post for chaining
     */
    public JobPost removeSkill(Skill skill) {
        skills.remove(skill);
        skill.getJobPosts().remove(this);
        return this;
    }

    /**
     * Adds a tag to the job post
     * 
     * @param tag the tag to add
     * @return this job post for chaining
     */
    public JobPost addTag(Tag tag) {
        tags.add(tag);
        tag.getJobPosts().add(this);
        return this;
    }

    /**
     * Removes a tag from the job post
     * 
     * @param tag the tag to remove
     * @return this job post for chaining
     */
    public JobPost removeTag(Tag tag) {
        tags.remove(tag);
        tag.getJobPosts().remove(this);
        return this;
    }

    /**
     * Checks if this job post is active and visible
     * 
     * @return true if the job post is active and not expired
     */
    public boolean isActive() {
        return active && (visibleUntil == null || visibleUntil.isAfter(LocalDateTime.now()));
    }

    /**
     * Activates the job post
     */
    public void activate() {
        this.active = true;
    }

    /**
     * Deactivates the job post
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Gets the formatted salary range
     * 
     * @return the formatted salary range
     */
    public String getSalaryRangeFormatted() {
        if (minSalary == null && maxSalary == null) {
            return "Negotiable";
        } else if (minSalary != null && maxSalary != null) {
            return minSalary + " - " + maxSalary + " " + (currency != null ? currency : "");
        } else if (minSalary != null) {
            return "From " + minSalary + " " + (currency != null ? currency : "");
        } else {
            return "Up to " + maxSalary + " " + (currency != null ? currency : "");
        }
    }
} 