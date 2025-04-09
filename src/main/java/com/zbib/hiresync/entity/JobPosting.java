package com.zbib.hiresync.entity;

import com.zbib.hiresync.enums.EmploymentType;
import com.zbib.hiresync.enums.JobStatus;
import com.zbib.hiresync.enums.LocationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing a job posting in the system. Contains all details about a position including
 * requirements, responsibilities, and compensation information.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "job_postings")
public class JobPosting implements Serializable {
  private static final long serialVersionUID = 1L;

  /** Unique identifier for the job posting. */
  @Id @GeneratedValue private UUID id;

  /** User who created/owns this job posting. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /** Job title/position name. */
  @Column(nullable = false, length = 100)
  private String title;

  /** Department or team where the position is located. */
  @Column(nullable = false, length = 50)
  private String department;

  /** Detailed description of the position. */
  @Column(nullable = false, length = 2000, columnDefinition = "TEXT")
  private String description;

  /** Key responsibilities of the position. */
  @Column(nullable = false, length = 2000, columnDefinition = "TEXT")
  private String responsibilities;

  /** Required qualifications for candidates. */
  @Column(nullable = false, length = 2000, columnDefinition = "TEXT")
  private String qualifications;

  /** Benefits offered with the position. */
  @Column(length = 1000, columnDefinition = "TEXT")
  private String benefits;

  /** Required years of experience. */
  @Column(nullable = false)
  private int yearsOfExperience;

  /** Location type (REMOTE, HYBRID, ONSITE). */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private LocationType locationType;

  /** Employment type (FULL_TIME, PART_TIME, CONTRACT, etc.). */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EmploymentType employmentType;

  /** Current status of the job posting. */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private JobStatus status;

  /** Minimum salary offered. */
  private int minSalary;

  /** Maximum salary offered. */
  private int maxSalary;

  /** Timestamp when the job posting was created. */
  @Column(nullable = false)
  private LocalDateTime createdAt;

  /** Timestamp when the job posting was last updated. */
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  /** Sets creation timestamp before persisting. */
  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  /** Updates the updatedAt timestamp before updates. */
  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
