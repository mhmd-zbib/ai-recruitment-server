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
    private UUID jobId;
    private String searchTerm;
    private ApplicationStatus status;
    private LocalDateTime appliedDateFrom;
    private LocalDateTime appliedDateTo;
    private String referredBy;
    private Integer page;
    private Integer size;
    private Sort.Direction sortDirection;
    private String sortBy;

    public static class Fields {
        public static final String JOB_ID = "job.id";
        public static final String STATUS = "status";
        public static final String APPLIED_AT = "appliedAt";
        public static final String REFERRED_BY = "referredBy";
        public static final String FIRST_NAME = "firstName";
        public static final String LAST_NAME = "lastName";
        public static final String EMAIL = "email";
    }
}