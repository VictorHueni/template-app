/**
 * Domain infrastructure - base entity classes and JPA configuration.
 *
 * <p><strong>PUBLIC API:</strong></p>
 * <ul>
 *   <li>{@link com.example.demo.common.domain.AbstractBaseEntity} - Base class for all domain entities with TSID and JPA auditing</li>
 * </ul>
 *
 * <p><strong>Used by:</strong> ALL domain modules (greeting, user, audit)</p>
 *
 * <p>This package provides the foundational entity infrastructure that all domain
 * modules extend. It ensures consistent entity identification using TSIDs and
 * automatic auditing of created/updated timestamps and users.</p>
 *
 * @since 1.0.0
 */
package com.example.demo.common.domain;
