package com.example.demo.common.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * AuditorAware implementation that resolves the current user for JPA auditing.
 *
 * <p>Handles multiple principal types:</p>
 * <ul>
 *   <li>UserDetailsImpl (direct domain entity - returned directly)</li>
 *   <li>Spring Security UserDetails (extracts username, looks up in repository)</li>
 *   <li>String username (from JWT or other auth providers)</li>
 * </ul>
 *
 * <p>Falls back to "system" user for unauthenticated operations (batch jobs, startup).</p>
 *
 * <p><strong>Note:</strong> The system user is cached at startup to avoid triggering
 * Hibernate auto-flush during preUpdate callbacks, which would cause StackOverflowError.</p>
 */
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(auth -> auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken))
                .map(Authentication::getName)
                .or(() -> Optional.of("anonymous"));
    }
}