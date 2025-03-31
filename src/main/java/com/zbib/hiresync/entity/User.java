package com.zbib.hiresync.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {

  @Id @GeneratedValue private UUID id;

  @Column(unique = true, nullable = false, length = 50)
  private String username;

  @Column(nullable = false, length = 255)
  private String password;

  @Column(nullable = false, length = 20)
  private String role = "USER";
}
