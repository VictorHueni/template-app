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
public class CustomRevisionListener implements RevisionListener { // 1. Implement RevisionListener

    @Override
    public void newRevision(Object revisionEntity) {
        CustomRevisionEntity entity = (CustomRevisionEntity) revisionEntity;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Case 1: Internal Job (No Security Context) -> "system"
        if (auth == null) {
            entity.setUsername("system");
            return;
        }

        // Case 2: Unauthenticated Public User -> "anonymous"
        if (auth instanceof AnonymousAuthenticationToken) {
            entity.setUsername("anonymous");
            return;
        }

        // Case 3: Authenticated User -> Username
        if (auth.isAuthenticated()) {
            String name = auth.getName();
            // Safety fallback if principal has no name
            entity.setUsername(name != null ? name : "unknown");
            return;
        }

        // Fallback for edge cases (Authenticated=false but not AnonymousToken)
        entity.setUsername("unknown");
    }
}