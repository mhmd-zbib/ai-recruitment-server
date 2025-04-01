package com.zbib.hiresync.entity;

import com.zbib.hiresync.enums.ApplicationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a job application submitted by a candidate. Tracks application details
 * including resume, cover letter, and status.
 */
@Entity
@Table(name = "applications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Application {

  @Id @GeneratedValue private UUID id;

  @ManyToOne
  @JoinColumn(name = "job_id", nullable = false)
  private Job job;

  @Column(nullable = false)
  private String firstName;

  @Column(nullable = false)
  private String lastName;

  @Column(nullable = false)
  private String email;

  @Column(nullable = true)
  private String phoneNumber;

  @Column(nullable = true)
  private String websiteUrl;

  @Column(nullable = true)
  private String linkedinUrl;

  @Column(nullable = true)
  private String cvUrl;

  @Column(nullable = true)
  private String referredBy;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ApplicationStatus status;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;
}
