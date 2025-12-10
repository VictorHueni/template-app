package com.example.demo.common.audit;

import org.hibernate.envers.RevisionListener;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Revision listener that captures the current user's username for audit revisions.
 *
 * <p>This listener is invoked by Hibernate Envers whenever a new revision is created
 * (on INSERT, UPDATE, or DELETE of audited entities). It extracts the username from
 * the Spring Security context and stores it in the {@link CustomRevisionEntity}.</p>
 *
 * <p>Falls back to "system" when no authentication is present (batch jobs, startup).</p>
 */
public class CustomRevisionListener implements RevisionListener {

    private static final String SYSTEM_USER = "system";

    @Override
    public void newRevision(Object revisionEntity) {
        CustomRevisionEntity rev = (CustomRevisionEntity) revisionEntity;
        rev.setUsername(getCurrentUsername());
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return SYSTEM_USER;
        }

        String name = authentication.getName();
        return name != null ? name : SYSTEM_USER;
    }
}
