package com.zbib.hiresync.entity;

import com.zbib.hiresync.enums.ApplicationStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "applications")
public class Application {

  @Id @GeneratedValue private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_id", nullable = false)
  private Job job;

  @Column(nullable = false, length = 50)
  private String firstName;

  @Column(nullable = false, length = 50)
  private String lastName;

  @Column(nullable = false, length = 100)
  private String email;

  @Column(nullable = false, length = 20)
  private String phoneNumber;

  @Column(length = 255)
  private String linkedinUrl;

  @Column(length = 255)
  private String websiteUrl;

  @Column(nullable = false, length = 255)
  private String cvUrl;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ApplicationStatus status;

  @Column(length = 100)
  private String referredBy;

  @Column(nullable = false)
  private LocalDateTime createdAt;
}
