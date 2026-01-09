package com.example.demo.testsupport.persistence;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DelegatingDataSource;

public final class SmartRoutingDataSource extends DelegatingDataSource {

    private static final Logger log = LoggerFactory.getLogger(SmartRoutingDataSource.class);

    public SmartRoutingDataSource(DataSource targetDataSource) {
        super(Objects.requireNonNull(targetDataSource, "targetDataSource"));
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = super.getConnection();
        applySearchPath(connection);
        return wrapToResetOnClose(connection);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = super.getConnection(username, password);
        applySearchPath(connection);
        return wrapToResetOnClose(connection);
    }

    private static void applySearchPath(Connection connection) throws SQLException {
        String schema = SchemaContext.getSchema();
        if (schema == null) {
            log.debug("No schema context, resetting search path to default");
            resetSearchPath(connection);
            return;
        }

        String quotedSchema = quoteIdentifier(schema);
        log.debug("Applying search_path to schema: {}", schema);
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO " + quotedSchema + ", public");
        }
    }

    private static void resetSearchPath(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("RESET search_path");
        }
    }

    private static String quoteIdentifier(String identifier) {
        Objects.requireNonNull(identifier, "identifier");
        if (identifier.isBlank()) {
            throw new IllegalArgumentException("identifier must not be blank");
        }

        String escaped = identifier.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private static Connection wrapToResetOnClose(Connection connection) {
        InvocationHandler handler = new ResetSearchPathOnCloseInvocationHandler(connection);
        return (Connection) Proxy.newProxyInstance(
                SmartRoutingDataSource.class.getClassLoader(),
                new Class<?>[] {Connection.class},
                handler);
    }

    private static final class ResetSearchPathOnCloseInvocationHandler implements InvocationHandler {

        private final Connection delegate;

        private ResetSearchPathOnCloseInvocationHandler(Connection delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("close") && method.getParameterCount() == 0) {
                SQLException resetException = null;
                try {
                    resetSearchPath(delegate);
                } catch (SQLException ex) {
                    resetException = ex;
                }

                try {
                    delegate.close();
                } catch (SQLException closeException) {
                    if (resetException != null) {
                        resetException.addSuppressed(closeException);
                        throw resetException;
                    }
                    throw closeException;
                }

                if (resetException != null) {
                    throw resetException;
                }
                return null;
            }

            try {
                return method.invoke(delegate, args);
            } catch (ReflectiveOperationException ex) {
                Throwable cause = ex.getCause();
                if (cause != null) {
                    throw cause;
                }
                throw ex;
            }
        }
    }
}
