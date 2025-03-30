package com.zbib.hiresync.dto;

import com.zbib.hiresync.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicationFilter {
    private String query;
    private ApplicationStatus status;
    private LocalDateTime minCreatedAt;
    private LocalDateTime maxCreatedAt;
}