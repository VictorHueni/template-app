/**
 * Common module - Shared utilities and base classes.
 *
 * <p>This module is marked as OPEN because it provides shared infrastructure
 * (AbstractBaseEntity, FunctionalIdGenerator, etc.) that is used across all
 * other modules.</p>
 *
 * <p>Components in this module:</p>
 * <ul>
 *   <li>AbstractBaseEntity - Base entity with TSID and JPA auditing</li>
 *   <li>FunctionalIdGenerator - Business-friendly ID generation</li>
 *   <li>CustomRevisionEntity - Envers audit metadata</li>
 *   <li>Exception handlers and security configuration</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
        type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package com.example.demo.common;
