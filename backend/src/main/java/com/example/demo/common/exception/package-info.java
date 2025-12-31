/**
 * Exception handling infrastructure.
 *
 * <p><strong>PUBLIC API:</strong></p>
 * <ul>
 *   <li>{@link com.example.demo.common.exception.DomainException} - Base class for all module-specific domain exceptions</li>
 *   <li>{@link com.example.demo.common.exception.ProblemDetailFactory} - RFC 7807 Problem Detail creation utility</li>
 *   <li>{@link com.example.demo.common.exception.ProblemType} - Standard problem type URI constants and builders</li>
 * </ul>
 *
 * <p><strong>LEGACY (deprecated):</strong></p>
 * <ul>
 *   <li>{@link com.example.demo.common.exception.ResourceNotFoundException} - Generic not-found exception (use module-specific exceptions instead)</li>
 *   <li>{@link com.example.demo.common.exception.BusinessValidationException} - Generic validation exception (use module-specific exceptions instead)</li>
 *   <li>{@link com.example.demo.common.exception.ConflictException} - Generic conflict exception (use module-specific exceptions instead)</li>
 * </ul>
 *
 * <p><strong>Used by:</strong> ALL modules for exception handling</p>
 *
 * <p>This package provides the foundational exception handling infrastructure that
 * enables modules to define their own domain-specific exceptions while maintaining
 * consistent RFC 7807 Problem Details responses.</p>
 *
 * <p><strong>Module Autonomy Pattern:</strong></p>
 * <p>Modules should extend {@link com.example.demo.common.exception.DomainException}
 * to create their own exception types (e.g., {@code GreetingNotFoundException}) and
 * handle them with module-specific {@code @RestControllerAdvice} handlers.</p>
 *
 * @since 1.0.0
 */
package com.example.demo.common.exception;
