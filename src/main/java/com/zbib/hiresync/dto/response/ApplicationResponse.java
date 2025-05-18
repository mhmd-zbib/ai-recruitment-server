package com.zbib.hiresync.dto.response;

import com.zbib.hiresync.enums.ApplicationStatus;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String applicantEmail;
    private String resumeUrl;
    private String linkedinUrl;
    private ApplicationStatus status;
    private String notes;
    private int matchRate;
    private String summary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}