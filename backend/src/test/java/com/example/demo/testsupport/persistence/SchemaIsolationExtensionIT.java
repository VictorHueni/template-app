package com.example.demo.testsupport.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    classes = SchemaIsolationExtensionIT.TestApp.class,
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
            + "org.springframework.modulith.core.config.ApplicationModuleInitializerRuntimeVerification,"
    })
@ActiveProfiles({"test", "integration"})
@ExtendWith(SchemaIsolationExtension.class)
class SchemaIsolationExtensionIT {

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {HibernateJpaAutoConfiguration.class})
    @Import({TestcontainersConfiguration.class, TestPersistenceConfig.class})
    static class TestApp {
        // Intentionally empty: minimal bootstrapped context for datasource + jdbc only.
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsSchema_setsContext_runsMigrations() {
        String schema = SchemaContext.getSchema();
        assertThat(schema).isNotBlank();
        assertThat(schema).startsWith("test_req_");

        String currentSchema = jdbcTemplate.queryForObject("select current_schema()", String.class);
        assertThat(currentSchema).isEqualTo(schema);

        Integer flywayTableCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = current_schema() and table_name = 'flyway_schema_history'",
                Integer.class);
        assertThat(flywayTableCount).isEqualTo(1);
    }
}
