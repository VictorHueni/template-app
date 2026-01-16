package com.example.demo.testsupport.persistence;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter that propagates test schema context from HTTP headers to the request thread.
 *
 * <p><strong>Purpose:</strong> In integration tests using RestAssured with {@code RANDOM_PORT},
 * the test logic runs on the JUnit thread while the controller logic runs on a Tomcat thread.
 * This filter reads the {@code X-Test-Schema} header and sets the {@link SchemaContext} so that
 * all database operations during the request use the correct test schema.</p>
 *
 * <p><strong>Critical: Schema Context Lifecycle</strong></p>
 * <p>This filter does NOT clear the schema context after request processing. This is intentional
 * because Spring Modulith's {@code @ApplicationModuleListener} (which combines {@code @Async},
 * {@code @Transactional}, and {@code @TransactionalEventListener}) processes events asynchronously
 * AFTER the HTTP response is sent:</p>
 *
 * <pre>
 * Timeline:
 * 1. HTTP Request arrives → Filter sets SchemaContext
 * 2. Controller processes request, service creates entity, publishes event
 * 3. Transaction commits → triggers @TransactionalEventListener
 * 4. filterChain.doFilter() returns → HTTP response sent
 * 5. [If we cleared here, async tasks would lose schema context!]
 * 6. Spring Modulith async executor picks up event → TaskDecorator captures schema
 * 7. Async event handler processes event with correct schema context
 * </pre>
 *
 * <p><strong>Schema cleanup is handled by {@link SchemaIsolationExtension#afterEach}:</strong></p>
 * <ul>
 *   <li>Test awaits async operations to complete (via Awaitility assertions)</li>
 *   <li>Clears SchemaContext</li>
 *   <li>Drops the test schema CASCADE</li>
 * </ul>
 *
 * @see SchemaContext
 * @see SchemaContextTaskDecorator
 * @see SchemaIsolationExtension
 */
public class TestSchemaFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TestSchemaFilter.class);

    public static final String TEST_SCHEMA_HEADER = "X-Test-Schema";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String schema = request.getHeader(TEST_SCHEMA_HEADER);

        if (schema != null && !schema.isBlank()) {
            log.debug("Setting schema context from header: {}", schema);
            SchemaContext.setSchema(schema);
        } else {
            // Clear any stale schema from previous requests on this pooled thread.
            // This prevents requests without the X-Test-Schema header from accidentally
            // using a schema that was set by a previous test and may have been dropped.
            String existingSchema = SchemaContext.getSchema();
            if (existingSchema != null) {
                log.debug("Clearing stale schema context '{}' (no header in request)", existingSchema);
                SchemaContext.clear();
            }
        }

        // Process the request - schema context remains set for async event processing
        // IMPORTANT: We intentionally do NOT clear SchemaContext here.
        // Async event handlers (Spring Modulith @ApplicationModuleListener) need the schema
        // context to be available when TaskDecorator.decorate() is called, which happens
        // AFTER this filter completes but BEFORE SchemaIsolationExtension.afterEach() runs.
        filterChain.doFilter(request, response);
    }
}
