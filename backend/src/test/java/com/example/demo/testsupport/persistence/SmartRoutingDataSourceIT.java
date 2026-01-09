package com.example.demo.testsupport.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class SmartRoutingDataSourceIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @AfterEach
    void afterEach() {
        SchemaContext.clear();
    }

    @Test
    void routesSearchPathPerThreadAndResetsOnClose() throws SQLException {
        HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig());
        SmartRoutingDataSource dataSource = new SmartRoutingDataSource(hikariDataSource);

        try {
            createSchema(hikariDataSource, "test_schema_1");
            createSchema(hikariDataSource, "test_schema_2");

            SchemaContext.setSchema("test_schema_1");
            assertThat(currentSchema(dataSource.getConnection())).isEqualTo("test_schema_1");

            SchemaContext.setSchema("test_schema_2");
            assertThat(currentSchema(dataSource.getConnection())).isEqualTo("test_schema_2");

            SchemaContext.clear();
            assertThat(currentSchema(dataSource.getConnection())).isEqualTo("public");
        } finally {
            hikariDataSource.close();
        }
    }

    private static HikariConfig hikariConfig() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(POSTGRES.getJdbcUrl());
        config.setUsername(POSTGRES.getUsername());
        config.setPassword(POSTGRES.getPassword());
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(1);
        config.setPoolName("SmartRoutingDataSourceIT");
        return config;
    }

    private static void createSchema(HikariDataSource dataSource, String schemaName) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS \"" + schemaName + "\"");
        }
    }

    private static String currentSchema(Connection connection) throws SQLException {
        try (connection) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery("SELECT current_schema()")) {
                    resultSet.next();
                    return resultSet.getString(1);
                }
            }
        }
    }
}
