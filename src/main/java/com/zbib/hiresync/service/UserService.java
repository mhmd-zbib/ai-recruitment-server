package com.zbib.hiresync.service;

import java.util.UUID;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.repository.UserRepository;
import com.zbib.hiresync.security.UserDetailsImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    return new UserDetailsImpl(user);
  }

  public User getUserById(UUID id) {
    return userRepository.findById(id).orElseThrow(null);
  }

  public UserResponse createUser(UserRequest userRequest) {
    // ... existing code ...
  }

  public UserResponse getUserById(UUID id) {
    // ... existing code ...
  }

  public UserResponse updateUser(UUID id, UserRequest userRequest) {
    // ... existing code ...
  }

  public void deleteUser(UUID id) {
    // ... existing code ...
  }

  public User getUserByEmail(String email) {
    // ... existing code ...
  }
}
