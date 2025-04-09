package com.zbib.hiresync.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApplicationRequest {

  @NotBlank(message = "First name is required")
  @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
  private String firstName;

  @NotBlank(message = "Last name is required")
  @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
  private String lastName;

  @NotBlank(message = "Email is required")
  @Email(message = "Please provide a valid email address")
  private String email;

  @NotBlank(message = "Phone number is required")
  @Pattern(
      regexp = "^[+]?[(]?[0-9]{1,4}[)]?[-\\s.]?[0-9]{1,4}[-\\s.]?[0-9]{1,9}$",
      message = "Please provide a valid phone number")
  private String phoneNumber;

  @URL(message = "Please provide a valid LinkedIn URL")
  private String linkedInUrl;

  @URL(message = "Please provide a valid website URL")
  private String websiteUrl;

  @NotBlank(message = "CV URL is required")
  @URL(message = "Please provide a valid CV URL")
  private String cvUrl;

  private String referredBy;
}
