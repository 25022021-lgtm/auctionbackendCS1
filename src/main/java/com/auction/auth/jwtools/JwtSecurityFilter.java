package com.auction.auth.jwtools;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.auction.auth.RevokedToken;
import com.auction.auth.RevokedTokenRepository;
import com.auction.auth.exceptions.JwtExpiredException;
import com.auction.users.UserService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

//The actual jwt filter, extending OncePerRequestFilter means that this filer will only get run through once in the entire process.
@Component
public class JwtSecurityFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RevokedTokenRepository revokedTokenRepository;
    private final UserService userService;
    private final HandlerExceptionResolver resolver;
    public JwtSecurityFilter(
        JwtUtil jwtUtil,
        UserService userService,
        RevokedTokenRepository revokedTokenRepository,
        @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver
    ) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.revokedTokenRepository = revokedTokenRepository;
        this.resolver = resolver;
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
        // handle authorization
        boolean isTokenValidated;
        try {
            isTokenValidated = jwtUtil.validateJwtToken(encodedToken);
        } catch (JwtExpiredException e) {
            resolver.resolveException(request, response, null, e);
            isTokenValidated = false;
        }

        if (encodedToken != null && isTokenValidated) {
            String username = jwtUtil.getUserFromToken(encodedToken);
            Date issuedAt = jwtUtil.getIssuedAtFromToken(encodedToken);

            Optional<RevokedToken> revoked = revokedTokenRepository.findById(
                username
            );

            if (revoked.isPresent()) {
                // if the time of issued is before the time of the revoke -> user is banned.
                // else if the time of the issue is after the time of the revoke -> means that the user has been unbanned (since they were logged in)
                if (
                    !issuedAt
                        .toInstant()
                        .isAfter(
                            Instant.ofEpochMilli(revoked.get().getBannedAt())
                        )
                ) {
                    filterChain.doFilter(request, response);
                    return;
                }
                revokedTokenRepository.delete(revoked.get());
            }

            UserDetailsImpl userDetails = UserDetailsImpl.JPAtoUserDetails(
                userService.getUserByUsername(username)
            );

            // authenticationToken is a warapper for UserDetailsImpl
            UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
                );
            authenticationToken.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
            );
            // adds auth credentials.
            SecurityContextHolder.getContext().setAuthentication(
                authenticationToken
            );
        }
        // Continue the fiterChain
        filterChain.doFilter(request, response);
    }

    // Checks basic structure of jwt
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
