package com.shortlink.security.util;

import com.shortlink.exception.AuthException;
import com.shortlink.security.user.UserPrincipal;
import com.shortlink.user.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

// Utility class providing static access to the currently authenticated user from the SecurityContext.
public final class SecurityUtils {

    private SecurityUtils() {
    }

    // Retrieves the currently authenticated User entity from the SecurityContext.
    public static User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUser();
        }
        throw new AuthException("Authentication required to perform this action");
    }

    // Optionally retrieves the currently authenticated User entity, or returns null if unauthenticated.
    public static User getCurrentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUser();
        }
        return null;
    }
}
