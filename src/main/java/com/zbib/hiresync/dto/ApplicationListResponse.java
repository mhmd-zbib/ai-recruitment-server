package com.zbib.hiresync.dto;

import com.zbib.hiresync.enums.ApplicationStatus;
import com.zbib.hiresync.enums.JobStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class ApplicationListResponse {

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private ApplicationStatus status;
    private LocalDateTime createdAt;

    private ApplicationJobResponse job;

    @Getter
    @Setter
    @Builder
    public static class ApplicationJobResponse {
        private UUID id;
        private String title;
        private String department;
        private JobStatus status;
    }
}
