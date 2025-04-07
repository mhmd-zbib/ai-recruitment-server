package com.zbib.hiresync.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a user in the system. Users can be candidates, recruiters, or administrators.
 * This entity stores basic user information and credentials.
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements Serializable {
  private static final long serialVersionUID = 1L;

  /** Unique identifier for the user. */
  @Id @GeneratedValue private UUID id;

  /** Username for login. */
  private String username;

  /** Email address for contact and notifications. */
  private String email;

  /** Hashed password for authentication. */
  private String password;

  /** User role (e.g., ADMIN, RECRUITER, CANDIDATE). */
  private String role;
}
