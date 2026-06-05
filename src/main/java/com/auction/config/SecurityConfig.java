package com.auction.config;

import com.auction.auth.jwtools.JwtSecurityFilter;
import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtSecurityFilter jwtSecurityFilter;
    public static 
    public SecurityConfig(JwtSecurityFilter jwtSecurityFilter) {
        this.jwtSecurityFilter = jwtSecurityFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(
            Arrays.asList("http://localhost:5173", "http://localhost:3000")
        ); // React
        // default
        // ports
        configuration.setAllowedMethods(
            Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );
        configuration.setAllowedHeaders(
            Arrays.asList("Authorization", "content-type", "x-auth-token")
        );
        configuration.setExposedHeaders(Arrays.asList("x-auth-token"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s ->
                s.sessionCreationPolicy(
                    org.springframework.security.config.http.SessionCreationPolicy.STATELESS
                )
            )
            .authorizeHttpRequests(auth ->
                auth
                    // Permit access to public endpoints
                    .requestMatchers(
                        "/users/login",
                        "/swagger-ui/**",
                        "/swagger.json",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/register",
                        "/login",
                        "/refresh"
                    )
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/items/**",
                        "/item/status/**"
                    )
                    .permitAll()
                    // Admin endpoints restricted to ADMIN role
                    .requestMatchers("/admin/**")
                    .hasRole("ADMIN")
                    // Authenticate all other requests
                    .anyRequest()
                    .authenticated()
            );
        // Add jwt filter into the security chain.
        http.addFilterBefore(
            jwtSecurityFilter,
            UsernamePasswordAuthenticationFilter.class
        );
        return http.build();
    }
}
