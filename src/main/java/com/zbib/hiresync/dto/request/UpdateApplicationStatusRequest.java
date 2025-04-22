package com.zbib.hiresync.dto.request;

import com.zbib.hiresync.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating application status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateApplicationStatusRequest {
    
    @NotNull(message = "Application status is required")
    private ApplicationStatus status;
    
    @Size(max = 5000, message = "Notes must be less than 5000 characters")
    private String notes;
} 