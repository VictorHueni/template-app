package com.example.demo.testsupport.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskDecorator;

/**
 * Propagates schema context from the submitting thread to async task execution threads.
 *
 * <p>This decorator is essential for Spring Modulith's {@code @ApplicationModuleListener}
 * which executes asynchronously via {@code @Async}. Without this decorator, async event
 * handlers would not inherit the schema context and database writes would go to the
 * wrong schema (or fail).</p>
 *
 * <p><strong>How it works:</strong></p>
 * <ol>
 *   <li>When a task is submitted, capture the current thread's schema context</li>
 *   <li>Return a wrapper Runnable that sets the schema before execution</li>
 *   <li>Clear the schema context in finally block to prevent ThreadLocal pollution</li>
 * </ol>
 */
public class SchemaContextTaskDecorator implements TaskDecorator {

    private static final Logger log = LoggerFactory.getLogger(SchemaContextTaskDecorator.class);

    @Override
    public Runnable decorate(Runnable runnable) {
        String schema = SchemaContext.getSchema();
        if (schema == null) {
            log.debug("No schema context to propagate - task will run without schema routing");
            return runnable;
        }

        log.debug("Capturing schema context for async propagation: {}", schema);

        return () -> {
            log.debug("Executing async task with schema context: {}", schema);
            try {
                SchemaContext.setSchema(schema);
                runnable.run();
            }
            finally {
                SchemaContext.clear();
                log.debug("Cleared schema context after async task completion");
            }
        };
    }
}
