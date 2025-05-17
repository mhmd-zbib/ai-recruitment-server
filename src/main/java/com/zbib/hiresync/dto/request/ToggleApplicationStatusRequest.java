package com.zbib.hiresync.dto.request;

import com.zbib.hiresync.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToggleApplicationStatusRequest {
    
    @NotNull(message = "Status is required")
    private ApplicationStatus status;
    
    private String notes;
}