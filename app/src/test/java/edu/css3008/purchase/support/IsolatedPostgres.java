package edu.css3008.purchase.support;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

public final class IsolatedPostgres {
    private static String isolatedUrl;

    private IsolatedPostgres() {}

    public static synchronized String url() {
        if (isolatedUrl == null) {
            isolatedUrl = newSchemaUrl();
        }
        return isolatedUrl;
    }

    public static String newSchemaUrl() {
        String baseUrl = requiredEnvironment("DB_URL");
        if (!baseUrl.startsWith("jdbc:postgresql:") || baseUrl.contains("currentSchema=")) {
            throw new IllegalStateException("DB_URL must be a PostgreSQL JDBC URL without currentSchema");
        }
        String schema = "css3008_test_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(baseUrl, username(), password());
             var statement = connection.createStatement()) {
            // The identifier contains a fixed prefix and generated hexadecimal characters only.
            statement.execute("CREATE SCHEMA " + schema);
        } catch (SQLException failure) {
            throw new IllegalStateException("Cannot create an isolated PostgreSQL test schema. "
                    + "Start PostgreSQL and check DB_URL, DB_USERNAME, and DB_PASSWORD.", failure);
        }
        return baseUrl + (baseUrl.contains("?") ? "&" : "?") + "currentSchema=" + schema;
    }

    public static String username() {
        return requiredEnvironment("DB_USERNAME");
    }

    public static String password() {
        return requiredEnvironment("DB_PASSWORD");
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Set " + name + " before running PostgreSQL integration tests");
        }
        return value;
    }
}
