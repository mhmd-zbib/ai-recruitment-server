package com.zbib.hiresync.dto;

import com.zbib.hiresync.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationFilter {
    private String searchTerm;
    private ApplicationStatus status;
    private LocalDateTime appliedDateFrom;
    private LocalDateTime appliedDateTo;
    private String referredBy;
    private Integer page;
    private Integer size;
}