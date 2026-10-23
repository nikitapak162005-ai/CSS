package edu.css3008.purchase.persistence;

import edu.css3008.purchase.support.IsolatedPostgres;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FlywayMigrationIT {
    @Test
    void versionTwoPreservesExistingRequestsAndMigrationsAreRepeatableWithoutChanges() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                IsolatedPostgres.newSchemaUrl(), IsolatedPostgres.username(), IsolatedPostgres.password());
        Flyway firstVersion = Flyway.configure().dataSource(dataSource)
                .target("1").baselineOnMigrate(false).outOfOrder(false).cleanDisabled(true).load();
        assertEquals(1, firstVersion.migrate().migrationsExecuted);

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO purchase_request (id, business_key, status, title) VALUES (?, ?, ?, ?)",
                id, "PR-LEGACY", "DRAFT", "Existing office chairs");

        Flyway latest = Flyway.configure().dataSource(dataSource)
                .baselineOnMigrate(false).outOfOrder(false).cleanDisabled(true)
                .validateMigrationNaming(true).load();
        assertEquals(1, latest.migrate().migrationsExecuted);
        assertEquals("UNASSIGNED", jdbc.queryForObject(
                "SELECT cost_center FROM purchase_request WHERE id = ?", String.class, id));
        assertEquals("Existing office chairs", jdbc.queryForObject(
                "SELECT title FROM purchase_request WHERE id = ?", String.class, id));
        assertEquals(List.of("1", "2"), jdbc.queryForList(
                "SELECT version FROM flyway_schema_history ORDER BY installed_rank", String.class));

        latest.validate();
        assertEquals(0, latest.migrate().migrationsExecuted);
        assertEquals(2L, jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history", Long.class));
        assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM purchase_request", Long.class));
    }
}
