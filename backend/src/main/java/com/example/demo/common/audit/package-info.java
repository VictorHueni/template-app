/**
 * Audit infrastructure - Hibernate Envers configuration.
 *
 * <p><strong>INTERNAL:</strong></p>
 * <ul>
 *   <li>{@link com.example.demo.common.audit.CustomRevisionEntity} - Envers revision tracking entity</li>
 *   <li>{@link com.example.demo.common.audit.CustomRevisionListener} - Revision metadata population</li>
 * </ul>
 *
 * <p><strong>Used by:</strong> JPA infrastructure (auto-discovered by Envers)</p>
 *
 * <p>This package configures Hibernate Envers for entity auditing, capturing
 * who made changes and when. The revision entity tracks metadata for each
 * transaction that modifies audited entities.</p>
 *
 * <p><strong>Note:</strong> This is internal infrastructure - modules should not
 * directly interact with these classes. Envers automatically discovers and uses them.</p>
 *
 * @since 1.0.0
 */
package com.example.demo.common.audit;
