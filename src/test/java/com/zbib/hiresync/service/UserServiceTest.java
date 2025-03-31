package com.zbib.hiresync.service;

import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.repository.UserRepository;
import com.zbib.hiresync.security.UserDetailsImpl;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class UserServiceTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private UserService userService;

  private User testUser;
  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    testUser = new User();
    testUser.setId(userId);
    testUser.setUsername("testuser");
    testUser.setPassword("password");
    testUser.setRole("USER");
  }

  @Test
  void loadUserByUsername_ExistingUser_ReturnsUserDetails() {
    // Arrange
    Mockito.when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

    // Act
    UserDetails userDetails = userService.loadUserByUsername("testuser");

    // Assert
    Assertions.assertNotNull(userDetails);
    Assertions.assertEquals("testuser", userDetails.getUsername());
    Assertions.assertTrue(userDetails instanceof UserDetailsImpl);
    Mockito.verify(userRepository, Mockito.times(1)).findByUsername("testuser");
  }

  @Test
  void loadUserByUsername_NonExistingUser_ThrowsException() {
    // Arrange
    Mockito.when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

    // Act & Assert
    Exception exception =
        Assertions.assertThrows(
            UsernameNotFoundException.class, () -> userService.loadUserByUsername("nonexistent"));

    Assertions.assertEquals("User not found", exception.getMessage());
    Mockito.verify(userRepository, Mockito.times(1)).findByUsername("nonexistent");
  }

  @Test
  void getUserById_ExistingUser_ReturnsUser() {
    // Arrange
    Mockito.when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

    // Act
    User user = userService.getUserById(userId);

    // Assert
    Assertions.assertNotNull(user);
    Assertions.assertEquals(userId, user.getId());
    Assertions.assertEquals("testuser", user.getUsername());
    Mockito.verify(userRepository, Mockito.times(1)).findById(userId);
  }

  @Test
  void getUserById_NonExistingUser_ThrowsException() {
    // Arrange
    UUID nonExistentId = UUID.randomUUID();
    Mockito.when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

    // Act & Assert
    Assertions.assertThrows(
        NullPointerException.class, () -> userService.getUserById(nonExistentId));

    Mockito.verify(userRepository, Mockito.times(1)).findById(nonExistentId);
  }
}
