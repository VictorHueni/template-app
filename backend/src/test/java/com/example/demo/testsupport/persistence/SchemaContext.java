package com.example.demo.testsupport.persistence;

import java.util.Objects;

public final class SchemaContext {

    private static final ThreadLocal<String> SCHEMA = new ThreadLocal<>();

    private SchemaContext() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void setSchema(String schema) {
        Objects.requireNonNull(schema, "schema");
        if (schema.isBlank()) {
            throw new IllegalArgumentException("schema must not be blank");
        }
        SCHEMA.set(schema);
    }

    public static String getSchema() {
        return SCHEMA.get();
    }

    public static String requireSchema() {
        String schema = SCHEMA.get();
        if (schema == null) {
            throw new IllegalStateException("SchemaContext is not set for this thread");
        }
        return schema;
    }

    public static void clear() {
        SCHEMA.remove();
    }
}
