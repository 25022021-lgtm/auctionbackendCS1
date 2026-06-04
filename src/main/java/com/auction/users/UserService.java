package com.auction.users;

import com.auction.common.BaseException;
import com.auction.common.BaseObjectResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserBalanceSink userBalanceSink;

    public UserService(
        UserRepository userRepository,
        UserBalanceSink userBalanceSink
    ) {
        this.userRepository = userRepository;
        this.userBalanceSink = userBalanceSink;
    }

    @Transactional
    public User getUserReferenceByUsername(String username) {
        User userRef = userRepository.getReferenceById(username);
        return userRef;
    }

    @Transactional
    public Double addBalance(String username, Double amount) {
        User user = getUserByUsername(username);
        user.addBalance(amount);
        userRepository.save(user);
        userBalanceSink.pushNewBalance(username, user.getBalance());
        return user.getBalance();
    }

    @Transactional
    public void deductBalance(String username, Double amount) {
        User user = getUserByUsername(username);
        user.deductBalance(amount);
        userRepository.save(user);
        userBalanceSink.pushNewBalance(username, user.getBalance());
    }

    @Transactional
    public BaseObjectResponse<Double> depositCredit(
        String username,
        Double creditAmount
    ) {
        Double newBalance = addBalance(username, creditAmount);
        return new BaseObjectResponse<>(
            true,
            "Succesfully deposited credit, current balance",
            newBalance
        );
    }

    @Transactional(readOnly = true)
    public BaseObjectResponse<Double> getBalance(String username) {
        User user = userRepository
            .findByUsername(username)
            .orElseThrow(() -> new BaseException("Invalid username"));
        return new BaseObjectResponse<>(
            true,
            "Get balance successful",
            user.getBalance()
        );
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
