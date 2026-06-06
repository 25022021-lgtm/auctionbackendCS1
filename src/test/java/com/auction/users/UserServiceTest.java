package com.auction.users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.auction.common.BaseException;
import com.auction.common.BaseObjectResponse;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private UserBalanceSink userBalanceSink;

  @InjectMocks private UserService userService;

  private User testUser;

  @BeforeEach
  void setUp() {
    testUser = new User("testuser", "Test User", "password", 100.0);
  }

  @Test
  void addBalance_Success() {
    // Arrange
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    doNothing().when(userBalanceSink).pushNewBalance(anyString(), any(Double.class));

    // Act
    Double newBalance = userService.addBalance("testuser", 50.0);

    // Assert
    assertEquals(150.0, newBalance);
    assertEquals(150.0, testUser.getBalance());
    verify(userRepository).save(testUser);
    verify(userBalanceSink).pushNewBalance("testuser", 150.0);
  }

  @Test
  void deductBalance_Success() {
    // Arrange
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    doNothing().when(userBalanceSink).pushNewBalance(anyString(), any(Double.class));

    // Act
    userService.deductBalance("testuser", 30.0);

    // Assert
    assertEquals(70.0, testUser.getBalance());
    verify(userRepository).save(testUser);
    verify(userBalanceSink).pushNewBalance("testuser", 70.0);
  }

  @Test
  void depositCredit_Success() {
    // Arrange
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    doNothing().when(userBalanceSink).pushNewBalance(anyString(), any(Double.class));

    // Act
    BaseObjectResponse<Double> response = userService.depositCredit("testuser", 25.0);

    // Assert
    assertTrue(response.getStatus());
    assertEquals(125.0, response.getEntity());
    assertEquals("Succesfully deposited credit, current balance", response.getMessage());
  }

  @Test
  void getBalance_Success() {
    // Arrange
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

    // Act
    BaseObjectResponse<Double> response = userService.getBalance("testuser");

    // Assert
    assertTrue(response.getStatus());
    assertEquals(100.0, response.getEntity());
    assertEquals("Get balance successful", response.getMessage());
  }

  @Test
  void getBalance_UserNotFound_ThrowsException() {
    // Arrange
    when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

    // Act & Assert
    BaseException exception =
        assertThrows(
            BaseException.class,
            () -> {
              userService.getBalance("nonexistent");
            });
    assertEquals("Invalid username", exception.getMessage());
  }

  @Test
  void getUserByUsername_Success() {
    // Arrange
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

    // Act
    User foundUser = userService.getUserByUsername("testuser");

    // Assert
    assertEquals(testUser, foundUser);
  }

  @Test
  void getUserByUsername_UserNotFound_ThrowsException() {
    // Arrange
    when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

    // Act & Assert
    BaseException exception =
        assertThrows(
            BaseException.class,
            () -> {
              userService.getUserByUsername("nonexistent");
            });
    assertEquals("User not found", exception.getMessage());
  }
}
