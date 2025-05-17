package com.zbib.hiresync.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a professional skill in the system
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "skills",
    indexes = {
        @Index(name = "idx_skill_name", columnList = "name"),
        @Index(name = "idx_skill_category", columnList = "category")
    }
)
public class Skill {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 50, unique = true)
    private String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "category", length = 50)
    private String category;

    @ManyToMany(mappedBy = "skills")
    @Builder.Default
    private Set<Job> jobs = new HashSet<>();

} 