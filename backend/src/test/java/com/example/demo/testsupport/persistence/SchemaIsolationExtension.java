package com.example.demo.testsupport.persistence;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit.jupiter.SpringExtension;

public final class SchemaIsolationExtension implements BeforeEachCallback, AfterEachCallback {

    private static final Logger log = LoggerFactory.getLogger(SchemaIsolationExtension.class);

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(SchemaIsolationExtension.class);

    private static final String STORE_KEY_SCHEMA = "schema";

    private static final String SCHEMA_PREFIX = "test_req_";

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        SchemaContext.clear();

        DataSource dataSource = SpringExtension.getApplicationContext(context).getBean(DataSource.class);

        String schemaName = generateSchemaName();
        context.getStore(NAMESPACE).put(STORE_KEY_SCHEMA, schemaName);

        createSchema(dataSource, schemaName);

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
