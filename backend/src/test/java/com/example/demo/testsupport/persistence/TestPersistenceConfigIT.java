package com.example.demo.testsupport.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.example.demo.testsupport.TestcontainersConfiguration;

@SpringBootTest(
    classes = TestPersistenceConfigIT.TestApp.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "spring.autoconfigure.exclude="
            + "com.c4_soft.springaddons.security.oidc.starter.synchronised.resourceserver.SpringAddonsOidcResourceServerBeans,"
            + "org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration,"
            + "org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration,"
            + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,"
            // Spring Modulith requires @SpringBootApplication or JPA; exclude for minimal DataSource-only test
            + "org.springframework.modulith.runtime.autoconfigure.SpringModulithRuntimeAutoConfiguration,"
            + "org.springframework.modulith.observability.autoconfigure.ModuleObservabilityAutoConfiguration,"
            + "org.springframework.modulith.events.jpa.JpaEventPublicationAutoConfiguration,"
            + "org.springframework.modulith.events.config.EventPublicationAutoConfiguration,"
            + "org.springframework.modulith.actuator.autoconfigure.ApplicationModulesEndpointConfiguration,"
            + "org.springframework.modulith.core.config.ApplicationModuleInitializerRuntimeVerification",
    })
@ActiveProfiles({"test", "integration"})
class TestPersistenceConfigIT {

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {HibernateJpaAutoConfiguration.class})
    @Import({TestcontainersConfiguration.class, TestPersistenceConfig.class})
    static class TestApp {
        // Intentionally empty: minimal bootstrapped context for datasource + jdbc only.
    }

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void afterEach() {
        SchemaContext.clear();
    }

    @Test
    void primaryDataSourceIsSmartRoutingDataSource() {
        assertThat(dataSource).isInstanceOf(SmartRoutingDataSource.class);
    }

    @Test
    void schemaContextControlsCurrentSchema() {
        String defaultSchema = jdbcTemplate.queryForObject("select current_schema()", String.class);
        assertThat(defaultSchema).isEqualTo("public");

        String schema = "schema_test_persistence_config_it";
        jdbcTemplate.execute("create schema if not exists " + schema);

        SchemaContext.setSchema(schema);
        String schemaAfterSet = jdbcTemplate.queryForObject("select current_schema()", String.class);
        assertThat(schemaAfterSet).isEqualTo(schema);

        SchemaContext.clear();
        String schemaAfterClear = jdbcTemplate.queryForObject("select current_schema()", String.class);
        assertThat(schemaAfterClear).isEqualTo("public");
    }
}
