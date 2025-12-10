package com.example.demo.common.config;

import com.example.demo.user.domain.UserDetailsImpl;
import com.example.demo.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

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
public class AuditorAwareImpl implements AuditorAware<UserDetailsImpl> {

    private static final String SYSTEM = "system";
    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Cached system user to avoid DB lookups during Hibernate callbacks.
     * This prevents StackOverflowError from auto-flush cascade during preUpdate.
     */
    private volatile UserDetailsImpl cachedSystemUser;

    public AuditorAwareImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Initializes the cached system user after application is fully ready.
     * This ensures data.sql has executed and the system user exists.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void initSystemUser() {
        if (cachedSystemUser == null) {
            userRepository.findByUsername(SYSTEM).ifPresent(user -> this.cachedSystemUser = user);
        }
    }

    @Override
    public Optional<UserDetailsImpl> getCurrentAuditor() {
        return Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken))
                .flatMap(this::resolveUser)
                .or(this::getSystemUser);
    }

    /**
     * Returns the cached system user to avoid triggering Hibernate auto-flush.
     * Uses EntityManager with FlushModeType.COMMIT to prevent auto-flush cascade.
     */
    private Optional<UserDetailsImpl> getSystemUser() {
        if (cachedSystemUser != null) {
            return Optional.of(cachedSystemUser);
        }

        // Use EntityManager with COMMIT flush mode to avoid auto-flush trigger
        try {
            TypedQuery<UserDetailsImpl> query = entityManager
                    .createQuery("SELECT u FROM UserDetailsImpl u WHERE u.username = :username", UserDetailsImpl.class)
                    .setParameter("username", SYSTEM)
                    .setFlushMode(FlushModeType.COMMIT);  // Prevents auto-flush during preUpdate callback
            UserDetailsImpl user = query.getSingleResult();
            this.cachedSystemUser = user;
            return Optional.of(user);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    /**
     * Resolves the user from authentication.
     * If principal is already UserDetailsImpl, returns it directly (no DB lookup).
     */
    private Optional<UserDetailsImpl> resolveUser(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        // If principal is already our domain entity, return it directly
        if (principal instanceof UserDetailsImpl domainUser) {
            return Optional.of(domainUser);
        }

        // Otherwise, extract username and look up
        String username = extractUsername(principal, authentication);
        return userRepository.findByUsername(username);
    }

    /**
     * Extracts username from principal.
     */
    private String extractUsername(Object principal, Authentication authentication) {
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        } else if (principal instanceof String username) {
            return username;
        }
        return authentication.getName();
    }
}
