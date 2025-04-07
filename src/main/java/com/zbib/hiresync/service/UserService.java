package com.zbib.hiresync.service;

import com.zbib.hiresync.dto.UserRequest;
import com.zbib.hiresync.dto.UserResponse;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.repository.UserRepository;
import com.zbib.hiresync.security.UserDetailsImpl;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public final class UserService implements UserDetailsService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  public UserDetails loadUserByUsername(String username) {
    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    return new UserDetailsImpl(user);
  }

  public User getUserById(UUID id) {
    return userRepository
        .findById(id)
        .orElseThrow(() -> new NoSuchElementException("User not found with id: " + id));
  }

  public User getUserByEmail(String email) {
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new NoSuchElementException("User not found with email: " + email));
  }
  
  public UserResponse createUser(UserRequest userRequest) {
    User user = User.builder()
        .username(userRequest.getUsername())
        .email(userRequest.getEmail())
        .password(passwordEncoder.encode(userRequest.getPassword()))
        .role(userRequest.getRole())
        .build();
    
    User savedUser = userRepository.save(user);
    return mapToUserResponse(savedUser);
  }
  
  public UserResponse getUserResponseById(UUID id) {
    User user = getUserById(id);
    return mapToUserResponse(user);
  }
  
  public UserResponse updateUser(UUID id, UserRequest userRequest) {
    User existingUser = getUserById(id);
    
    existingUser.setUsername(userRequest.getUsername());
    existingUser.setEmail(userRequest.getEmail());
    
    if (userRequest.getPassword() != null && !userRequest.getPassword().isEmpty()) {
      existingUser.setPassword(passwordEncoder.encode(userRequest.getPassword()));
    }
    
    if (userRequest.getRole() != null) {
      existingUser.setRole(userRequest.getRole());
    }
    
    User updatedUser = userRepository.save(existingUser);
    return mapToUserResponse(updatedUser);
  }
  
  private UserResponse mapToUserResponse(User user) {
    return UserResponse.builder()
        .id(user.getId())
        .username(user.getUsername())
        .email(user.getEmail())
        .role(user.getRole())
        .build();
  }
}
