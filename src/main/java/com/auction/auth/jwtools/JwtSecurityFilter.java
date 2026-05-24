package com.auction.auth.jwtools;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Security filter that validates JWT tokens on incoming requests and sets the authentication
 * context.
 */
@Component
public class JwtSecurityFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;
  private final CustomUserDetailsService userDetailsService;

  public JwtSecurityFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
    this.jwtUtil = jwtUtil;
    this.userDetailsService = userDetailsService;
  }

  /**
   * Filters incoming requests to validate the JWT token and set the authentication context.
   */
  @Override
  public void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String encodedToken = parseJwt(request);
    // Allow unauthenticated requests to proceed, let controller-level security
    // handle authorization.
    if (encodedToken != null && jwtUtil.validateJwtToken(encodedToken)) {
      String username = jwtUtil.getUserFromToken(encodedToken);
      UserDetailsImpl userDetails = userDetailsService.loadUserByUsername(username);
      UsernamePasswordAuthenticationToken authenticationToken =
          new UsernamePasswordAuthenticationToken(
              userDetails, null, userDetails.getAuthorities());
      authenticationToken.setDetails(
          new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }
    filterChain.doFilter(request, response);
  }

  /** Parses the JWT token from the Authorization header. */
  public String parseJwt(HttpServletRequest request) {
    String authenticationHeader = request.getHeader("Authorization");

    if (authenticationHeader != null && authenticationHeader.startsWith("Bearer ")) {
      return authenticationHeader.substring(7);
    }
    return null;
  }
}
