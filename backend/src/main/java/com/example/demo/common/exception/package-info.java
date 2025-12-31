/**
 * Exception handling infrastructure providing two complementary patterns for error handling.
 *
 * <h2>Core Infrastructure</h2>
 * <ul>
 *   <li>{@link com.example.demo.common.exception.DomainException} - Base class for module-specific domain exceptions</li>
 *   <li>{@link com.example.demo.common.exception.ProblemDetailFactory} - RFC 7807 Problem Detail creation utility</li>
 *   <li>{@link com.example.demo.common.exception.ProblemType} - Standard problem type URI constants and builders</li>
 * </ul>
 *
 * <h2>Exception Handling Patterns</h2>
 *
 * <p>This template provides <strong>two valid approaches</strong> for exception handling. Choose based on your module's needs:</p>
 *
 * <h3>Pattern 1: Common Exceptions (Simple Scenarios)</h3>
 * <p>Use for straightforward CRUD operations and standard HTTP error patterns:</p>
 * <ul>
 *   <li>{@link com.example.demo.common.exception.ResourceNotFoundException} - Generic 404 not found</li>
 *   <li>{@link com.example.demo.common.exception.BusinessValidationException} - Generic 400 validation errors</li>
 *   <li>{@link com.example.demo.common.exception.ConflictException} - Generic 409 conflicts</li>
 * </ul>
 *
 * <p><strong>When to use common exceptions:</strong></p>
 * <ul>
 *   <li>Simple CRUD operations without complex domain logic</li>
 *   <li>Standard validation scenarios</li>
 *   <li>Generic resource lookups</li>
 *   <li>Quick prototyping or MVP development</li>
 *   <li>When error context is minimal (just ID and resource type)</li>
 * </ul>
 *
 * <p><strong>Example:</strong></p>
 * <pre>{@code
 * public User getUser(Long id) {
 *     return repository.findById(id)
 *         .orElseThrow(() -> new ResourceNotFoundException("User", id));
 * }
 * }</pre>
 *
 * <h3>Pattern 2: Module-Specific Exceptions (Complex Domain)</h3>
 * <p>Create by extending {@link DomainException} for domain-rich scenarios:</p>
 * <ul>
 *   <li>Define exception in {@code module.exception} package</li>
 *   <li>Implement {@code getHttpStatus()}, {@code getProblemTypeUri()}, {@code enrichProblemDetail()}</li>
 *   <li>Create module-specific {@code @RestControllerAdvice} handler</li>
 * </ul>
 *
 * <p><strong>When to use module-specific exceptions:</strong></p>
 * <ul>
 *   <li>Domain has specific error scenarios (e.g., PaymentDeclinedException)</li>
 *   <li>Need custom error properties beyond resourceType/resourceId</li>
 *   <li>Want to communicate domain concepts through error responses</li>
 *   <li>Module requires specialized error logging or handling</li>
 *   <li>Error handling is part of the domain model</li>
 * </ul>
 *
 * <p><strong>Example:</strong> See {@code com.example.demo.greeting.exception.GreetingNotFoundException}</p>
 * <pre>{@code
 * public class GreetingNotFoundException extends DomainException {
 *     private final Long greetingId;
 *     private final String greetingReference;
 *
 *     @Override
 *     public String getProblemTypeUri() {
 *         return ProblemType.buildModuleProblemType("greeting", "not-found");
 *     }
 *
 *     @Override
 *     public void enrichProblemDetail(ProblemDetail pd) {
 *         pd.setProperty("greetingId", greetingId);
 *         pd.setProperty("greetingReference", greetingReference);
 *     }
 * }
 * }</pre>
 *
 * <h2>Spring Modulith Compliance</h2>
 * <p><strong>Both patterns respect Spring Modulith principles:</strong></p>
 * <ul>
 *   <li>Common exceptions are <em>infrastructure</em>, not domain coupling</li>
 *   <li>Modules don't depend on each other through exceptions</li>
 *   <li>Exception handling is an internal implementation detail</li>
 *   <li>No module boundaries are violated by either approach</li>
 * </ul>
 *
 * <h2>Choosing Your Approach</h2>
 * <p>The choice between patterns is based on <strong>pragmatism, not dogma</strong>:</p>
 * <ul>
 *   <li><strong>Simple modules</strong> can use common exceptions without violating best practices</li>
 *   <li><strong>Complex modules</strong> benefit from domain-specific exceptions</li>
 *   <li><strong>Hybrid approach</strong> is acceptable - use common exceptions initially, evolve to module-specific as domain complexity grows</li>
 *   <li>This follows the same pattern used by Spring Framework itself (common DataAccessException + specific subclasses)</li>
 * </ul>
 *
 * <h2>Architecture</h2>
 * <p>All exceptions produce RFC 7807 Problem Details responses with:</p>
 * <ul>
 *   <li><code>type</code>: URI identifying the problem type</li>
 *   <li><code>title</code>: Short, human-readable summary</li>
 *   <li><code>status</code>: HTTP status code</li>
 *   <li><code>detail</code>: Human-readable explanation</li>
 *   <li><code>instance</code>: URI identifying the specific occurrence</li>
 *   <li><code>timestamp</code>: When the error occurred (ISO-8601)</li>
 *   <li><code>traceId</code>: Unique identifier for debugging</li>
 * </ul>
 *
 * @since 1.0.0
 */
package com.example.demo.common.exception;
