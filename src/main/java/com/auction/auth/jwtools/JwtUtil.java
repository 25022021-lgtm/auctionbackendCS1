package com.auction.auth.jwtools;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.logging.Logger;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Utility class for generating and validating JWT tokens. */
@Component
public class JwtUtil {

  private static final Logger logger = Logger.getLogger(JwtUtil.class.getName());

  @Value("${jwt.secret}")
  private String jwtSecret;

  @Value("${jwt.expiration}")
  private int jwtExpirationMs;

  @Value("${jwt.refreshExpiration}") // 7 days
  private int jwtRefreshExpirationMs;

  private SecretKey key;

  @PostConstruct
  public void init() {
    this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
  }

  /** Generates a short-lived access token for the given username. */
  public String generateToken(String username) {
    return Jwts.builder()
        .subject(username)
        .issuedAt(new Date())
        .expiration(new Date((new Date().getTime()) + jwtExpirationMs))
        .signWith(key)
        .compact();
  }

  /** Generates a long-lived refresh token for the given username. */
  public String generateRefreshToken(String username) {
    return Jwts.builder()
        .subject(username)
        .issuedAt(new Date())
        .expiration(new Date((new Date().getTime()) + jwtRefreshExpirationMs))
        .signWith(key)
        .compact();
  }

  /** Extracts the username (subject) from the given JWT token. */
  public String getUserFromToken(String token) {
    return Jwts.parser()
        .verifyWith(key)
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .getSubject();
  }

  /** Validates the given JWT token. Returns true if valid, false otherwise. */
  public boolean validateJwtToken(String token) {
    try {
      Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
      return true;
    } catch (Exception e) {
      logger.warning(String.format("JWT validation error: %s", e.getMessage()));
    }
    return false;
  }
}
