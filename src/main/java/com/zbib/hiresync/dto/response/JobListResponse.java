package com.zbib.hiresync.dto.response;

import com.zbib.hiresync.enums.EmploymentType;
import com.zbib.hiresync.enums.WorkplaceType;
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
public class JobListResponse {
    private UUID id;
    private String title;
    private WorkplaceType workplaceType;
    private EmploymentType employmentType;
    private boolean active;
    private long applicationCount;
    private LocalDateTime createdAt;
} 