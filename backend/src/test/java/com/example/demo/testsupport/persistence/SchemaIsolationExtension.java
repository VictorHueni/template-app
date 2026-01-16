package com.example.demo.testsupport.persistence;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.UUID;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * JUnit 5 extension that provides schema-per-test isolation for PostgreSQL integration tests.
 *
 * <p><strong>How it works:</strong></p>
 * <ol>
 *   <li>Creates a unique schema for each test method</li>
 *   <li>Runs Flyway migrations in that schema</li>
 *   <li>Sets the schema context for the test method execution</li>
 *   <li>Drops the schema CASCADE after the test completes</li>
 * </ol>
 *
 * <p><strong>Thread-safety for parallel execution:</strong></p>
 * <p>JUnit 5's parallel execution may run lifecycle callbacks (BeforeEach) on a different
 * thread than the test method. This extension handles this by:</p>
 * <ul>
 *   <li>Storing the schema name in ExtensionContext.Store (thread-safe)</li>
 *   <li>Using InvocationInterceptor to set SchemaContext at test method start</li>
 *   <li>This ensures the schema context is always set on the thread running the test</li>
 * </ul>
 */
public final class SchemaIsolationExtension
        implements BeforeEachCallback, AfterEachCallback, InvocationInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SchemaIsolationExtension.class);

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(SchemaIsolationExtension.class);

    private static final String STORE_KEY_SCHEMA = "schema";

    private static final String SCHEMA_PREFIX = "test_req_";

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        // Clear any stale schema context from previous tests
        SchemaContext.clear();

        // Generate the new schema name
        String schemaName = generateSchemaName();
        context.getStore(NAMESPACE).put(STORE_KEY_SCHEMA, schemaName);

        // Get DataSource BEFORE setting schema context
        // Schema creation needs default search_path, not the new schema
        DataSource dataSource = SpringExtension.getApplicationContext(context).getBean(DataSource.class);

        // Create the schema (uses default search_path since SchemaContext is null)
        createSchema(dataSource, schemaName);

        // NOW set schema context for all subsequent operations
        SchemaContext.setSchema(schemaName);

        log.info("Running Flyway migrations for schema '{}'", schemaName);
        Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .defaultSchema(schemaName)
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    /**
     * Intercepts test method invocation to ensure schema context is set on the executing thread.
     *
     * <p>This is critical for parallel execution where JUnit may run the test method on a
     * different thread than beforeEach. By wrapping the invocation, we guarantee the schema
     * context is properly set regardless of thread assignment.</p>
     */
    @Override
    public void interceptTestMethod(
            Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {

        String schemaName = extensionContext.getStore(NAMESPACE).get(STORE_KEY_SCHEMA, String.class);

        if (schemaName != null) {
            // Ensure schema context is set on THIS thread (the test method execution thread)
            String currentSchema = SchemaContext.getSchema();
            if (!schemaName.equals(currentSchema)) {
                log.debug("Setting schema context on test method thread: {} (was: {})", schemaName, currentSchema);
                SchemaContext.setSchema(schemaName);
            }
        }

        invocation.proceed();
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        String schemaName = context.getStore(NAMESPACE).get(STORE_KEY_SCHEMA, String.class);

        SchemaContext.clear();

        if (schemaName == null) {
            return;
        }

        dropSchema(context, schemaName);
    }

    private static String generateSchemaName() {
        String uuid = UUID.randomUUID().toString().replace("-", "").toLowerCase(Locale.ROOT);
        return SCHEMA_PREFIX + uuid;
    }

    private static void createSchema(DataSource dataSource, String schemaName) throws SQLException {
        String quotedSchema = quoteIdentifier(schemaName);
        log.info("Creating test schema {}", quotedSchema);
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + quotedSchema);
        }
    }

    private static void dropSchema(ExtensionContext context, String schemaName) throws SQLException {
        DataSource dataSource = SpringExtension.getApplicationContext(context).getBean(DataSource.class);

        String quotedSchema = quoteIdentifier(schemaName);
        log.info("Dropping test schema {} CASCADE", quotedSchema);
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA " + quotedSchema + " CASCADE");
        }
    }

    private static String quoteIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("identifier must not be blank");
        }
        String escaped = identifier.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
