package com.example.demo.common.audit;

import jakarta.persistence.*;

import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionEntity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

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
@NoArgsConstructor
@Getter
@Setter
public class CustomRevisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @RevisionNumber
    @Column(name = "rev")
    private int id;

    @RevisionTimestamp
    @Column(name = "revtstmp")
    private long timestamp;

    @Column(name = "username", length = 255)
    private String username;

}
