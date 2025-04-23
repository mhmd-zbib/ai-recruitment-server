package com.zbib.hiresync.domain;

import com.zbib.hiresync.enums.EmploymentType;
import com.zbib.hiresync.enums.WorkplaceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "workplace_type")
    private WorkplaceType workplaceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type")
    private EmploymentType employmentType;

    @Column(name = "min_salary")
    private BigDecimal minSalary;

    @Column(name = "max_salary")
    private BigDecimal maxSalary;

    private String currency;

    private boolean active;

    @Column(name = "visible_until")
    private LocalDateTime visibleUntil;

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Formula("(SELECT COUNT(a.id) FROM applications a WHERE a.job_id = id)")
    private Integer applicationCount;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "job_skills",
            joinColumns = @JoinColumn(name = "job_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<Skill> skills = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "job_tags",
            joinColumns = @JoinColumn(name = "job_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Application> applications = new HashSet<>();

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    public void incrementViewCount() {
        this.viewCount = (this.viewCount == null) ? 1 : this.viewCount + 1;
    }
    
    public void addSkill(Skill skill) {
        this.skills.add(skill);
    }
    
    public void removeSkill(Skill skill) {
        this.skills.remove(skill);
    }
    
    public void addTag(Tag tag) {
        this.tags.add(tag);
    }
    
    public void removeTag(Tag tag) {
        this.tags.remove(tag);
    }
} 