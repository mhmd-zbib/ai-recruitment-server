package com.zbib.hiresync.security;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.zbib.hiresync.entity.User;

import lombok.Getter;

/**
 * Implementation of UserDetails interface for user authentication.
 */
@Getter
public final class UserDetailsImpl implements UserDetails {

  private static final long serialVersionUID = 1L;
  private final User user;

  /**
   * Creates a new UserDetailsImpl instance.
   *
   * @param user the user entity
   */
  public UserDetailsImpl(final User user) {
    this.user = Objects.requireNonNull(user, "User cannot be null");
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
  }

  @Override
  public String getPassword() {
    return user.getPassword();
  }

  @Override
  public String getUsername() {
    return user.getEmail();
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  /**
   * Gets the user's ID.
   *
   * @return the user's ID
   */
  public UUID getId() {
    return user.getId();
  }

  /**
   * Gets the user entity.
   *
   * @return the user entity
   */
  public User getUser() {
    return user;
  }
}
