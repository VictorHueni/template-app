package com.example.demo.common.domain;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.envers.NotAudited;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Base Entity for all persistent domain objects.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>TSID (Time-Sorted Unique Identifier) - 50% storage saving over UUID, k-sortable</li>
 *   <li>JPA Auditing (@CreatedDate, @LastModifiedDate, @CreatedBy, @LastModifiedBy)</li>
 *   <li>Hibernate Envers ready - add @Audited to subclasses that need audit history</li>
 *   <li>Optimistic locking via @Version</li>
 * </ul>
 *
 * <p>To enable Envers audit history for an entity, add {@code @Audited} to the subclass:</p>
 * <pre>
 * {@code @Entity}
 * {@code @Audited}
 * public class MyEntity extends AbstractBaseEntity { ... }
 * </pre>
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AbstractBaseEntity implements Serializable {

    @Id
    @Tsid
    @Column(name = "id", nullable = false, updatable = false)
    @Getter
    private Long id;

    @Version
    @Column(name = "version")
    @Getter
    private Integer version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    @Getter
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    @Getter
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by_id", nullable = false, updatable = false)
    @NotAudited
    @Getter
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by_id")
    @NotAudited
    @Getter
    private String updatedBy;


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