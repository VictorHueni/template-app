/**
 * Repository infrastructure - ID generation and repository utilities.
 *
 * <p><strong>PUBLIC API:</strong></p>
 * <ul>
 *   <li>{@link com.example.demo.common.repository.FunctionalIdGenerator} - Business-readable ID generation service</li>
 * </ul>
 *
 * <p><strong>Used by:</strong> ALL domain modules needing functional references</p>
 *
 * <p>This package provides the infrastructure for generating business-friendly
 * functional references (e.g., "GR-2025-001") in addition to the TSID primary keys.
 * These references are human-readable and can be used in external communications.</p>
 *
 * @since 1.0.0
 */
package com.example.demo.common.repository;
