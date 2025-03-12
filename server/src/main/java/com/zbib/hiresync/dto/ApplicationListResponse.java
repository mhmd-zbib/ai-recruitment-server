package com.zbib.hiresync.dto;

import com.zbib.hiresync.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApplicationListResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String jobTitle;
    private UUID jobId;
    private ApplicationStatus status;
    private LocalDateTime appliedAt;
}