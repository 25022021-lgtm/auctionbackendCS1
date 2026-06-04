package com.auction.auth;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auction.auth.dto.AuthResponse;
import com.auction.auth.dto.LoginRequest;
import com.auction.auth.dto.RegisterRequest;
import com.auction.auth.jwtools.JwtUtil;
import com.auction.auth.jwtools.UserDetailsImpl;
import com.auction.common.BaseException;
import com.auction.common.BaseResponse;
import com.auction.users.User;
import com.auction.users.UserService;

@Service
public class AuthService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    @Value("${jwt.refreshExpiration}")
    private Long refreshLifetime;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    public AuthService(RefreshTokenRepository refreshTokenRepository, JwtUtil jwtUtil,
            PasswordEncoder passwordEncoder, UserService userService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
    }

    @Transactional
    public AuthResponse refreshingToken(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findRefreshTokenData(refreshToken)
                .orElseThrow(() -> new BaseException("invalid refresh token"));
        boolean isTokenExpired = token.getCreatedAt() + refreshLifetime < Instant.now().toEpochMilli();
        if (isTokenExpired) {
            throw new BaseException("Refresh token has expired, please login again");
        }
        String newRefreshToken = jwtUtil.generateRefreshToken(token.getUsername());
        token.setRefreshToken(newRefreshToken);
        String accessToken = jwtUtil.generateToken(token.getUsername());
        refreshTokenRepository.save(token);
        AuthResponse response = new AuthResponse(true, "successfully refreshed token", accessToken, newRefreshToken);
        return response;
    }

    public BaseResponse userRegister(RegisterRequest request) {
        if (userService.existsUsername(request.username())) {
            throw new BaseException("Username has already been taken");
        }
        String hashedPassword = passwordEncoder.encode(request.password());

        User user = new User(request.username(), request.displayName(), hashedPassword, 0.0);
        userService.saveUser(user);
        return new BaseResponse(true, "Successfully registered.");
    }

    @Transactional
    public AuthResponse loginUser(LoginRequest request) {

        User user = userService.getUserByUsername(request.username());

        if (!passwordEncoder.matches(request.password(), user.getHashedPassword())) {
            throw new BaseException("Invalid username or password");
        }

        String accessToken = jwtUtil.generateToken(user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        // Save the refresh token to the database
        refreshTokenRepository.save(new RefreshToken(user.getUsername(), refreshToken));
        AuthResponse response = new AuthResponse(true, "Successfully logged in.", accessToken, refreshToken);
        return response;
    }

    @Transactional
    public void revokeToken(String username) {
        refreshTokenRepository.deleteById(username);
    }

    @Transactional
    public BaseResponse logoutUser(UserDetailsImpl userDetailsImpl) {
        revokeToken(userDetailsImpl.getUsername());
        return new BaseResponse(true, "succesfully logout");
    }
}
