package com.shortlink.security;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.shortlink.config.CookieProperties;
import com.shortlink.config.FrontendProperties;
import com.shortlink.config.JwtProperties;
import com.shortlink.constants.SecurityEndpoints;
import com.shortlink.security.jwt.JwtAuthenticationFilter;
import com.shortlink.security.ratelimit.RateLimitingFilter;

import lombok.RequiredArgsConstructor;

// Spring Security configuration for the ShortLink application.
@Configuration
@EnableAutoConfiguration(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@EnableConfigurationProperties({JwtProperties.class, CookieProperties.class, FrontendProperties.class})
public class SecurityConfig {

    private final RateLimitingFilter rateLimitingFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityAuthenticationEntryPoint authenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SecurityEndpoints.PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, SecurityEndpoints.REDIRECT_ENDPOINT).permitAll()
                        .requestMatchers(SecurityEndpoints.ADMIN_ENDPOINTS).hasRole("ADMIN")
                        .requestMatchers(SecurityEndpoints.AUTHENTICATED_ENDPOINTS).authenticated()
                        .requestMatchers(SecurityEndpoints.PUBLIC_URL_ENDPOINTS).permitAll()
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
}
