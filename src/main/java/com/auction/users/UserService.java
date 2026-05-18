package com.auction.users;

import com.auction.common.BaseException;
import com.auction.users.dto.BalanceResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Transactional
  public User getUserReferenceByUsername(String username) {
    User userRef = userRepository.getReferenceById(username);
    return userRef;
  }

  @Transactional
  public BalanceResponse depositCredit(String username, Double creditAmount) {
    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new BaseException("Invalid username"));
    user.setBalance(user.getBalance() + creditAmount);
    user = userRepository.save(user);
    return new BalanceResponse(
        true, "Succesfully deposited credit, current balance", user.getBalance());
  }

  @Transactional(readOnly = true)
  public BalanceResponse getBalance(String username) {
    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new BaseException("Invalid username"));
    return new BalanceResponse(true, "Get balance successful", user.getBalance());
  }

  @Transactional(readOnly = true)
  public User getUserByUsername(String username) {
    return userRepository
        .findByUsername(username)
        .orElseThrow(() -> new BaseException("User not found"));
  }

  @Transactional(readOnly = true)
  public boolean existsUsername(String username) {
    boolean response = userRepository.existsByUsername(username);
    return response;
  }

  @Transactional
  public User saveUser(User user) {
    user = userRepository.save(user);
    return user;
  }

  @Transactional
  public User getUserRef(String username) {
    User userRef = userRepository.getReferenceById(username);
    return userRef;
  }
}
