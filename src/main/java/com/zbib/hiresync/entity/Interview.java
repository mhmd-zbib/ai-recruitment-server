package com.zbib.hiresync.entity;

import com.zbib.hiresync.enums.InterviewStatus;
import com.zbib.hiresync.enums.InterviewType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing an interview session between a recruiter and a candidate. Tracks interview
 * details, scheduling information, and outcomes.
 */
@Entity
@Table(name = "interviews")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Interview implements Serializable {
  private static final long serialVersionUID = 1L;

  /** Unique identifier for the interview. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  /** Job posting associated with this interview. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_id", nullable = false)
  private JobPosting job;

  /** User (candidate) being interviewed. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /** Title/name of the interview session. */
  @Column(nullable = false)
  private String title;

  /** Description of the interview. */
  @Column(nullable = false)
  private String description;

  /** When the interview is scheduled to take place. */
  @Column(nullable = false)
  private LocalDateTime scheduledAt;

  /** Duration of the interview in minutes. */
  @Column(nullable = false)
  private Integer duration;

  /** Physical location for the interview if applicable. */
  @Column(length = 100)
  private String location;

  /** Virtual meeting link for remote interviews. */
  @Column(length = 500)
  private String meetingLink;

  /** Type of interview (PHONE, VIDEO, IN_PERSON, etc.). */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private InterviewType type;

  /** Current status of the interview. */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private InterviewStatus status;

  /** Interviewer's notes about the candidate. */
  @Column(length = 1000)
  private String notes;

  /** External calendar event identifier. */
  @Column(length = 200)
  private String calendarEventId;

  /** Flag indicating if a reminder was sent to participants. */
  @Column(nullable = false)
  private boolean reminderSent;

  /** Timestamp when the interview record was created. */
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  /** Timestamp when the interview record was last updated. */
  @Column(name = "updated_at", nullable = false)
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
