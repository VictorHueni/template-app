package com.example.demo.common.domain;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;
import java.io.Serializable;
import java.util.Objects;

/**
 * Base Entity for all persistent domain objects.
 * * STRATEGY: TSID (Time-Sorted Unique Identifier)
 * * Benefits:
 * 1. Fits in a Long (64-bit) -> 50% storage saving over UUID (128-bit).
 * 2. k-sortable -> Indexes like an auto-increment integer (fast inserts).
 * 3. Collision-free -> Safe for distributed systems/microservices.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AbstractBaseEntity implements Serializable {

    @Id
    @Tsid
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Version
    @Column(name = "version")
    private Integer version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Getters
    public Long getId() { return id; }
    public Integer getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    /**
     * Effective Java equality pattern for Hibernate entities.
     * Checks equality based on the ID if it exists, otherwise object identity.
     */
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;

        // Handle Hibernate proxies
        Class<?> oEffectiveClass = o instanceof HibernateProxy ?
                ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ?
                ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();

        if (thisEffectiveClass != oEffectiveClass) return false;

        AbstractBaseEntity that = (AbstractBaseEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ?
                ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}