package com.example.demo.testsupport;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Centralized database cleanup utility for integration tests.
 *
 * <p><strong>Purpose:</strong> Ensures consistent and thorough cleanup between parallel tests,
 * preventing dirty data from leaking across test boundaries. This is critical when multiple
 * tests share the same database and run in parallel with resource locking.</p>
 *
 * <p><strong>Key Features:</strong></p>
 * <ul>
 *   <li>Disables PostgreSQL triggers during cleanup for performance</li>
 *   <li>Respects foreign key constraints with CASCADE delete</li>
 *   <li>Resets IDENTITY sequences to 1 for consistent IDs across test runs</li>
 *   <li>Handles tables with UUID primary keys (no RESTART IDENTITY needed)</li>
 *   <li>Re-enables triggers after cleanup completes (finally block guarantee)</li>
 * </ul>
 *
 * <p><strong>Usage - Controller/Integration Tests:</strong></p>
 * <pre>{@code
 * @Autowired
 * DatabaseCleanupHelper cleanupHelper;
 *
 * @BeforeEach
 * void setUp() {
 *     // Prepare clean state before test
 *     cleanupHelper.truncateTables("greeting");
 * }
 *
 * @AfterEach
 * void tearDown() {
 *     // Cleanup even if test fails (prevents dirty data leaking to next test)
 *     cleanupHelper.truncateTables("greeting");
 * }
 * }</pre>
 *
 * <p><strong>Usage - Service/Repository Tests with @Transactional:</strong></p>
 * <pre>{@code
 * @Transactional  // Automatic rollback - cleanup not needed
 * class GreetingServiceIT {
 *     // Tests automatically rolled back, no manual cleanup needed
 * }
 * }</pre>
 *
 * <p><strong>Important Notes:</strong></p>
 * <ul>
 *   <li>Use @BeforeEach to ensure tests start with clean data</li>
 *   <li>Use @AfterEach for non-@Transactional tests to prevent dirty data leakage</li>
 *   <li>For async event processing (Spring Modulith), cleanup @AfterEach is critical</li>
 *   <li>Controller tests (@SpringBootTest with REST) should NOT use @Transactional
 *       because HTTP requests execute in separate threads (transaction rollback ineffective)</li>
 * </ul>
 *
 * @see org.junit.jupiter.api.BeforeEach
 * @see org.junit.jupiter.api.AfterEach
 */
@Component
public class DatabaseCleanupHelper {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseCleanupHelper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Truncates all application tables in the correct order with proper cleanup.
     *
     * <p><strong>Operation Flow:</strong></p>
     * <ol>
     *   <li>Disables PostgreSQL triggers (SET session_replication_role = 'replica')</li>
     *   <li>Truncates tables in dependency order (child tables first, then parents)</li>
     *   <li>Uses CASCADE to handle foreign key constraints</li>
     *   <li>Resets IDENTITY sequences to 1 for consistent IDs</li>
     *   <li>Re-enables triggers in finally block (always executes)</li>
     * </ol>
     *
     * <p><strong>Performance Notes:</strong></p>
     * <ul>
     *   <li>Disabling triggers significantly speeds up TRUNCATE operations</li>
     *   <li>Ideal for @BeforeEach setup when all tables need cleaning</li>
     *   <li>Consider using {@link #truncateTables(String...)} for selective cleanup</li>
     * </ul>
     *
     * <p><strong>Thread Safety:</strong> This method is thread-safe because each test
     * acquires exclusive DB lock via @ResourceLock(value = "DB", mode = READ_WRITE)</p>
     *
     * @see #truncateTables(String...)
     */
    public void truncateAllTables() {
        // Disable triggers temporarily for performance
        // This prevents unnecessary trigger execution during bulk deletes
        jdbcTemplate.execute("SET session_replication_role = 'replica'");

        try {
            // Order matters: child tables first, then parents to respect foreign keys
            // CASCADE option handles foreign key dependencies automatically
            jdbcTemplate.execute("TRUNCATE TABLE greeting_aud CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE revinfo RESTART IDENTITY CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE business_activity_log RESTART IDENTITY CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE event_publication CASCADE");  // UUID primary key, no RESTART IDENTITY
            jdbcTemplate.execute("TRUNCATE TABLE greeting RESTART IDENTITY CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE app_user RESTART IDENTITY CASCADE");

            // Reset custom sequences to 1 for consistent ID generation across test runs
            // This ensures @GeneratedValue sequences start fresh for each test
            jdbcTemplate.execute("ALTER SEQUENCE seq_greeting_reference RESTART WITH 1");
            jdbcTemplate.execute("ALTER SEQUENCE seq_revinfo_id RESTART WITH 1");
        } finally {
            // Re-enable triggers (always executes, even if exception occurs)
            // This is critical to restore normal database behavior for subsequent operations
            jdbcTemplate.execute("SET session_replication_role = 'origin'");
        }
    }

    /**
     * Truncates specific tables for tests that only need partial cleanup.
     *
     * <p><strong>Use Cases:</strong></p>
     * <ul>
     *   <li>Controller tests that only modify specific tables (e.g., "greeting")</li>
     *   <li>Event-based tests that need to clean event_publication separately</li>
     *   <li>Faster cleanup when many tables are unaffected by the test</li>
     * </ul>
     *
     * <p><strong>Examples:</strong></p>
     * <pre>{@code
     * // Cleanup only greeting table
     * cleanupHelper.truncateTables("greeting");
     *
     * // Cleanup multiple related tables
     * cleanupHelper.truncateTables("business_activity_log", "greeting", "event_publication");
     * }</pre>
     *
     * <p><strong>Implementation Notes:</strong></p>
     * <ul>
     *   <li>event_publication table has UUID primary key (no RESTART IDENTITY)</li>
     *   <li>All other tables use IDENTITY sequences (require RESTART IDENTITY)</li>
     *   <li>CASCADE option automatically handles foreign key dependencies</li>
     *   <li>Triggers are disabled during operation (performance optimization)</li>
     * </ul>
     *
     * @param tableNames names of tables to truncate (e.g., "greeting", "event_publication")
     *
     * @see #truncateAllTables()
     */
    public void truncateTables(String... tableNames) {
        // Disable triggers temporarily for performance
        // This prevents unnecessary trigger execution during bulk deletes
        jdbcTemplate.execute("SET session_replication_role = 'replica'");

        try {
            for (String table : tableNames) {
                // Special handling for event_publication (UUID primary key, no IDENTITY sequence)
                // All other tables use IDENTITY and need RESTART IDENTITY to reset sequence
                if ("event_publication".equals(table)) {
                    jdbcTemplate.execute("TRUNCATE TABLE " + table + " CASCADE");
                } else {
                    jdbcTemplate.execute("TRUNCATE TABLE " + table + " RESTART IDENTITY CASCADE");
                }
            }
        } finally {
            // Re-enable triggers (always executes, even if exception occurs)
            // This is critical to restore normal database behavior for subsequent operations
            jdbcTemplate.execute("SET session_replication_role = 'origin'");
        }
    }
}
