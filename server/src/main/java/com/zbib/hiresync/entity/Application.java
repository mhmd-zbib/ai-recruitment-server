package com.zbib.hiresync.entity;

import com.zbib.hiresync.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    private Job job;

    private String firstName;

    private String lastName;

    private String email;

    private String phoneNumber;

    private String linkedInUrl;

    private String websiteUrl;

    private String cvUrl;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    private String referredBy;

    private LocalDateTime appliedAt;

}
