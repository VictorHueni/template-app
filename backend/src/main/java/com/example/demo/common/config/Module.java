package com.example.demo.common.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define the module scope for a JPA entity.
 * <p>
 * This is used by the PhysicalNamingStrategy to prefix table names.
 * Example: @Module("auth") on User entity -> table "auth_user".
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Module {
    /**
     * The module name (e.g., "auth", "billing", "sales").
     * This will be prefixed to the table name (e.g., "auth_user").
     * @return the module name
     */
    String value();
}
