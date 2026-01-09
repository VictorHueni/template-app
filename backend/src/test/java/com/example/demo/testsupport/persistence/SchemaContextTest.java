package com.example.demo.testsupport.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SchemaContextTest {

    @AfterEach
    void afterEach() {
        SchemaContext.clear();
    }

    @Test
    void setSchema_thenGetSchema_returnsSameValue() {
        SchemaContext.setSchema("test_schema_1");

        assertThat(SchemaContext.getSchema()).isEqualTo("test_schema_1");
    }

    @Test
    void clear_removesValue() {
        SchemaContext.setSchema("test_schema_1");

        SchemaContext.clear();

        assertThat(SchemaContext.getSchema()).isNull();
    }

    @Test
    void threadLocal_isolatedAcrossThreads() throws InterruptedException {
        SchemaContext.setSchema("test_schema_1");

        AtomicReference<String> otherThreadSchema = new AtomicReference<>();
        Thread thread = new Thread(() -> otherThreadSchema.set(SchemaContext.getSchema()));
        thread.start();
        thread.join();

        assertThat(otherThreadSchema.get()).isNull();
    }

    @Test
    void requireSchema_throwsWhenUnset() {
        SchemaContext.clear();

        assertThatThrownBy(SchemaContext::requireSchema)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SchemaContext is not set");
    }

    @Test
    void setSchema_rejectsBlank() {
        assertThatThrownBy(() -> SchemaContext.setSchema("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
