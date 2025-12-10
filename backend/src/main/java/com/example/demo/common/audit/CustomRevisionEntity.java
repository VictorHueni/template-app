package com.example.demo.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionEntity;

/**
 * Custom revision entity that captures the username who made the change.
 *
 * <p>Extends Hibernate Envers' DefaultRevisionEntity to add custom audit metadata.
 * The username is populated by {@link CustomRevisionListener} from the SecurityContext.</p>
 *
 * <p>Table structure:</p>
 * <ul>
 *   <li>id (REV) - Revision number (auto-increment)</li>
 *   <li>timestamp (REVTSTMP) - When the revision was created</li>
 *   <li>username - Who made the change</li>
 * </ul>
 */
@Entity
@Table(name = "revinfo")
@RevisionEntity(CustomRevisionListener.class)
public class CustomRevisionEntity extends DefaultRevisionEntity {

    @Column(name = "username", length = 255)
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
