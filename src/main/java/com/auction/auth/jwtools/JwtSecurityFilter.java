package com.auction.auth.jwtools;

import com.auction.auth.RevokedToken;
import com.auction.auth.RevokedTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtSecurityFilter extends OncePerRequestFilter {

    private JwtUtil jwtUtil;
    private CustomUserDetailsService userDetailsService;
    private RevokedTokenRepository revokedTokenRepository;

    public JwtSecurityFilter(
        JwtUtil jwtUtil,
        CustomUserDetailsService userDetailsService,
        RevokedTokenRepository revokedTokenRepository
    ) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.revokedTokenRepository = revokedTokenRepository;
    }

    /**
     * Filters incoming requests to validate the JWT token and set the
     * authentication context.
     */
    @Override
    public void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String encodedToken = parseJwt(request);
        // allow unauthenticated requests to proceed, let controller-level security
        // handle authorization
        if (encodedToken != null && jwtUtil.validateJwtToken(encodedToken)) {
            String username = jwtUtil.getUserFromToken(encodedToken);
            Date issuedAt = jwtUtil.getIssuedAtFromToken(encodedToken);

            Optional<RevokedToken> revoked = revokedTokenRepository.findById(username);
            if (revoked.isPresent()) {
                if (!issuedAt.toInstant().isAfter(Instant.ofEpochMilli(revoked.get().getBannedAt()))) {
                    filterChain.doFilter(request, response);
                    return;
                }
                revokedTokenRepository.delete(revoked.get());
            }

            UserDetailsImpl userDetails = userDetailsService.loadUserByUsername(
                username
            );
            UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
                );
            authenticationToken.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
            );
            SecurityContextHolder.getContext().setAuthentication(
                authenticationToken
            );
        }
        filterChain.doFilter(request, response);
    }

    public String parseJwt(HttpServletRequest request) {
        String authenticationHeader = request.getHeader("Authorization");

        if (
            authenticationHeader != null &&
            authenticationHeader.startsWith("Bearer ")
        ) {
            return authenticationHeader.substring(7);
        }
        return null;
    }
}
