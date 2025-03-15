package com.zbib.hiresync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobListResponse {
    private UUID id;
    private String title;
    private String department;
    private LocalDateTime createdAt;
    private int applications;
    private double match;
}