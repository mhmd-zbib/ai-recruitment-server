package com.zbib.hiresync.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.zbib.hiresync.enums.InterviewStatus;
import com.zbib.hiresync.enums.InterviewType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@Entity
@Table(name = "interviews")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Interview {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_id", nullable = false)
  private JobPosting job;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String description;

  @Column(nullable = false)
  private LocalDateTime scheduledAt;

  @Column(nullable = false)
  private Integer duration;

  @Column(length = 100)
  private String location;

  @Column(length = 500)
  private String meetingLink;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private InterviewType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private InterviewStatus status;

  @Column(length = 1000)
  private String notes;

  @Column(length = 200)
  private String calendarEventId;

  @Column(nullable = false)
  private boolean reminderSent;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
