package com.shortlink.security.jwt;

import com.shortlink.security.user.UserPrincipal;
import com.shortlink.user.User;
import com.shortlink.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Intercepts requests to validate Bearer JWT tokens and populate the Spring SecurityContext.
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwtToken = authHeader.substring(7).trim();

        if (jwtToken.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (jwtService.isTokenValid(jwtToken)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            String username = jwtService.extractUsername(jwtToken);

            if (username != null && !username.isBlank()) {
                try {
                    User user = userRepository.findByEmail(username).orElse(null);

                    if (user == null) {
                        log.warn(
                                "Authentication rejected: user [{}] not found in database",
                                username
                        );
                    } else {
                        UserPrincipal userPrincipal = new UserPrincipal(user);

                        if (!userPrincipal.isEnabled()) {
                            log.warn(
                                    "Authentication rejected: user account [{}] is disabled",
                                    username
                            );
                        } else if (!userPrincipal.isAccountNonLocked()) {
                            log.warn(
                                    "Authentication rejected: user account [{}] is locked or pending deletion",
                                    username
                            );
                        } else {
                            UsernamePasswordAuthenticationToken authToken =
                                    new UsernamePasswordAuthenticationToken(
                                            userPrincipal,
                                            null,
                                            userPrincipal.getAuthorities()
                                    );

                            authToken.setDetails(
                                    new WebAuthenticationDetailsSource()
                                            .buildDetails(request)
                            );

                            SecurityContextHolder
                                    .getContext()
                                    .setAuthentication(authToken);
                        }
                    }
                } catch (Exception ex) {
                    log.warn(
                            "Failed to load user authentication for [{}]: {}",
                            username,
                            ex.getMessage(),
                            ex
                    );
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}