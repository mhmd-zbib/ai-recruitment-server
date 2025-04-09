package com.zbib.hiresync.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import lombok.Data;

@Data
public class AuthRequest {

  @NotBlank(message = "Email cannot be blank")
  @Email(message = "Invalid email format")
  private String email;

  @NotBlank(message = "Password cannot be blank")
  @Pattern(
      regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,}$",
      message = "Password must be at least 8 characters long and contain at least one uppercase " +
                "letter, one lowercase letter, and one number.")
  private String password;
}
