package com.auction.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auction.auth.dto.AuthResponse;
import com.auction.auth.dto.LoginRequest;
import com.auction.auth.dto.RefreshTokenRequest;
import com.auction.auth.dto.RegisterRequest;
import com.auction.common.BaseResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse authResponse = authService.refreshingToken(request.refreshToken());
        return ResponseEntity.ok().body(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.loginUser(request);
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/register")
    public ResponseEntity<BaseResponse> register(@Valid @RequestBody RegisterRequest request) {
        BaseResponse response = authService.userRegister(request);
        return ResponseEntity.ok().body(response);
    }
}
