package com.zbib.hiresync.dto.request;

import com.zbib.hiresync.enums.ApplicationStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating job application details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateApplicationRequest {

    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String applicantName;

    @Email(message = "Must be a valid email address")
    @Size(max = 100, message = "Email must be less than 100 characters")
    private String applicantEmail;

    @Size(max = 20, message = "Phone number must be less than 20 characters")
    private String phoneNumber;

    @Size(max = 5000, message = "Cover letter must be less than 5000 characters")
    private String coverLetter;

    @Size(max = 255, message = "Resume URL must be less than 255 characters")
    private String resumeUrl;

    @Size(max = 255, message = "Portfolio URL must be less than 255 characters")
    private String portfolioUrl;
    
    @Size(max = 255, message = "LinkedIn URL must be less than 255 characters")
    private String linkedinUrl;
    
    private ApplicationStatus status;
    
    @Size(max = 5000, message = "Notes must be less than 5000 characters")
    private String notes;
} 