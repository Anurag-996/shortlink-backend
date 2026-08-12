package com.shortlink.security;

import com.shortlink.config.CookieProperties;
import com.shortlink.config.FrontendProperties;
import com.shortlink.config.JwtProperties;
import com.shortlink.constants.SecurityEndpoints;
import com.shortlink.security.jwt.JwtAuthenticationFilter;
import com.shortlink.security.ratelimit.RateLimitingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// Spring Security configuration for the ShortLink application.
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({JwtProperties.class, CookieProperties.class, FrontendProperties.class})
public class SecurityConfig {

    private final RateLimitingFilter rateLimitingFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityAuthenticationEntryPoint authenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SecurityEndpoints.PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, SecurityEndpoints.REDIRECT_ENDPOINT).permitAll()
                        .requestMatchers(SecurityEndpoints.PUBLIC_URL_ENDPOINTS).permitAll()
                        .requestMatchers(SecurityEndpoints.ADMIN_ENDPOINTS).hasRole("ADMIN")
                        .requestMatchers(SecurityEndpoints.AUTHENTICATED_ENDPOINTS).authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
