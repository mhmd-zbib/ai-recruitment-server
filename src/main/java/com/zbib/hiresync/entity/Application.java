package com.zbib.hiresync.entity;

import com.zbib.hiresync.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "applicant_email", nullable = false, length = 100)
    private String applicantEmail;

    @Column(name = "resume_url")
    private String resumeUrl;

    @Column(name = "linkedin_url")
    private String linkedinUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.SUBMITTED;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "match_rate")
    private int matchRate;

    @Column(name = "summary", length = 1000)
    private String summary;
    
    public boolean isActive() {
        return !isInTerminalState();
    }
    
    public boolean isInTerminalState() {
        return status == ApplicationStatus.REJECTED || 
               status == ApplicationStatus.WITHDRAWN || 
               status == ApplicationStatus.OFFER_ACCEPTED;
    }
    
    public void updateStatus(ApplicationStatus newStatus, String statusChangeNotes) {
        this.status = newStatus;
        
        if (statusChangeNotes != null && !statusChangeNotes.trim().isEmpty()) {
            if (this.notes == null || this.notes.trim().isEmpty()) {
                this.notes = statusChangeNotes;
            } else {
                this.notes = this.notes + "\n\n" + LocalDateTime.now() + " - Status changed to " + 
                        newStatus + ": " + statusChangeNotes;
            }
        }
    }
}