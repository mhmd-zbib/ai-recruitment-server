package com.zbib.hiresync.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateApplicationRequest {

    @NotNull(message = "Job ID is required")
    private UUID jobId;

    @NotBlank(message = "Applicant name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String applicantName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    @Size(max = 100, message = "Email must be less than 100 characters")
    private String applicantEmail;

    @Size(max = 20, message = "Phone number must be less than 20 characters")
    private String phoneNumber;

    @Size(max = 5000, message = "Cover letter must be less than 5000 characters")
    private String coverLetter;

    private String resumeUrl;

    private String portfolioUrl;
    
    private String linkedinUrl;
} 