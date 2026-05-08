package com.auction.users;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auction.auth.jwtools.UserDetailsImpl;
import com.auction.users.dto.BalanceResponse;
import com.auction.users.dto.DepositRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/me/deposit")
    public ResponseEntity<BalanceResponse> deposit(@AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
            @Valid @RequestBody DepositRequest request) {
        BalanceResponse response = userService.depositCredit(userDetailsImpl.getUsername(), request.amount());
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/me/balance")
    public ResponseEntity<BalanceResponse> balance(@AuthenticationPrincipal UserDetailsImpl userDetailsImpl) {
        BalanceResponse response = userService.getBalance(userDetailsImpl.getUsername());
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<User> profile(@AuthenticationPrincipal UserDetailsImpl userDetailsImpl) {
        User response = userService.getUserByUsername(userDetailsImpl.getUsername());
        return ResponseEntity.ok().body(response);
    }

}
