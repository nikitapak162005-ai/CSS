package edu.css3008.purchase.persistence;

import edu.css3008.purchase.config.Application;
import edu.css3008.purchase.domain.DuplicateRequest;
import edu.css3008.purchase.domain.PurchaseRequest;
import edu.css3008.purchase.domain.RequestId;
import edu.css3008.purchase.domain.RequestStatus;
import edu.css3008.purchase.domain.RequestStore;
import edu.css3008.purchase.dto.RegisterRequest;
import edu.css3008.purchase.service.RegistrationService;
import edu.css3008.purchase.support.IsolatedPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import jakarta.persistence.EntityManager;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Application.class)
class PostgresIntegrationIT {
    @Autowired
    private RegistrationService registration;
    @Autowired
    private RequestStore store;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager transactions;
    @Autowired
    private EntityManager entities;

    @DynamicPropertySource
    static void isolatedDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", IsolatedPostgres::url);
        registry.add("spring.datasource.username", IsolatedPostgres::username);
        registry.add("spring.datasource.password", IsolatedPostgres::password);
    }

    @BeforeEach
    void clearOnlyRowsInTheOwnedSchema() {
        String schema = jdbc.queryForObject("SELECT current_schema()", String.class);
        assertNotNull(schema);
        assertTrue(schema.matches("css3008_test_[a-f0-9]{32}"), "Tests must use their isolated schema");
        jdbc.update("DELETE FROM purchase_request");
    }

    @Test
    void secondStatementRollsBack() {
        DuplicateRequest failure = assertThrows(DuplicateRequest.class,
                () -> registration.insertTwice("PR-ROLLBACK"));

        assertNotNull(failure.getCause(), "The original unique constraint failure must be retained");
        assertEquals(0, registration.countByBusinessKey("PR-ROLLBACK"));
    }

    @Test
    void secondRequestKeepsTheFirst() {
        RequestId firstId = RequestId.newId();
        registration.register(new RegisterRequest(firstId, "PR-COMMITTED", "Office monitors"));

        assertThrows(DuplicateRequest.class, () -> registration.register(
                new RegisterRequest(RequestId.newId(), "PR-COMMITTED", "Duplicate office monitors")));

        assertEquals(1, registration.countByBusinessKey("PR-COMMITTED"));
        assertEquals(firstId.value(), jdbc.queryForObject(
                "SELECT id FROM purchase_request WHERE business_key = ?", UUID.class, "PR-COMMITTED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"DRAFT", "APPROVED", "ORDERED"})
    void jpaPersistsEachAllowedStatusAndAllRequiredFields(String statusCode) {
        Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        PurchaseRequest request = new PurchaseRequest(RequestId.newId(), "PR-" + statusCode,
                RequestStatus.fromCode(statusCode), "Laptop purchase", createdAt);
        TransactionTemplate transaction = new TransactionTemplate(transactions);
        transaction.executeWithoutResult(status -> store.insert(request));

        PurchaseRequest restored = jdbc.queryForObject("""
                SELECT id, business_key, status, title, created_at
                FROM purchase_request WHERE business_key = ?
                """, (result, row) -> new PurchaseRequest(new RequestId(result.getObject("id", UUID.class)),
                result.getString("business_key"), RequestStatusMapper.fromDatabase(result.getString("status")),
                result.getString("title"), result.getTimestamp("created_at").toInstant()), request.businessKey());

        assertEquals(request, restored);
        PurchaseRequest mapped = transaction.execute(status -> {
            RequestEntity entity = entities.find(RequestEntity.class, request.id().value());
            assertEquals("UNASSIGNED", entity.getCostCenter());
            return entity.toDomain();
        });
        assertEquals(request, mapped);
    }

    @Test
    void schemaHasTheRequiredColumnsAndConstraints() {
        List<String> columns = jdbc.queryForList("""
                SELECT column_name FROM information_schema.columns
                WHERE table_schema = current_schema() AND table_name = 'purchase_request'
                ORDER BY ordinal_position
                """, String.class);
        assertEquals(List.of("id", "business_key", "status", "title", "created_at", "cost_center"), columns);
        assertEquals(0L, jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.columns
                WHERE table_schema = current_schema() AND table_name = 'purchase_request'
                AND is_nullable <> 'NO'
                """, Long.class));
        assertEquals(List.of("uuid", "text", "text", "text", "timestamp with time zone", "text"),
                jdbc.queryForList("""
                        SELECT data_type FROM information_schema.columns
                        WHERE table_schema = current_schema() AND table_name = 'purchase_request'
                        ORDER BY ordinal_position
                        """, String.class));
        String statusConstraint = jdbc.queryForObject("""
                SELECT pg_get_constraintdef(oid) FROM pg_constraint
                WHERE conrelid = 'purchase_request'::regclass AND conname = 'purchase_request_status_check'
                """, String.class);
        assertNotNull(statusConstraint);
        for (String status : List.of("DRAFT", "APPROVED", "ORDERED")) {
            assertTrue(statusConstraint.contains("'" + status + "'"));
        }
        assertEquals(1L, jdbc.queryForObject("""
                SELECT count(*) FROM pg_constraint
                WHERE conrelid = 'purchase_request'::regclass AND contype = 'p'
                """, Long.class));
        assertEquals(1L, jdbc.queryForObject("""
                SELECT count(*) FROM pg_constraint
                WHERE conrelid = 'purchase_request'::regclass AND contype = 'u'
                """, Long.class));
    }

    @Test
    void databaseRejectsUnknownStatusesAndBlankRequiredText() {
        assertSqlState("23514", () -> insertRaw("PR-INVALID", "CANCELLED", "Office supplies"));
        assertSqlState("23514", () -> insertRaw(" ", "DRAFT", "Office supplies"));
        assertSqlState("23514", () -> insertRaw("PR-BLANK", "DRAFT", " "));
        assertSqlState("23502", () -> insertRaw(null, "DRAFT", "Office supplies"));
        assertSqlState("23502", () -> insertRaw("PR-NULL-STATUS", null, "Office supplies"));
        assertSqlState("23502", () -> insertRaw("PR-NULL", "DRAFT", null));
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM purchase_request", Long.class));
    }

    @Test
    void databaseSuppliesCreationTimeWhenItIsNotProvided() {
        Instant beforeInsert = Instant.now().minusSeconds(1);
        insertRaw("PR-DEFAULT", "DRAFT", "Office supplies");
        Timestamp createdAt = jdbc.queryForObject(
                "SELECT created_at FROM purchase_request WHERE business_key = ?", Timestamp.class, "PR-DEFAULT");
        assertNotNull(createdAt);
        assertFalse(createdAt.toInstant().isBefore(beforeInsert));
        assertFalse(createdAt.toInstant().isAfter(Instant.now().plusSeconds(1)));
        assertEquals("UNASSIGNED", jdbc.queryForObject(
                "SELECT cost_center FROM purchase_request WHERE business_key = ?", String.class, "PR-DEFAULT"));
    }

    @Test
    void flywayAppliesBothVersionsBeforeHibernateValidation() {
        assertEquals(List.of("1", "2"), jdbc.queryForList("""
                SELECT version FROM flyway_schema_history
                WHERE success = true AND version IS NOT NULL ORDER BY installed_rank
                """, String.class));
        assertEquals(0L, jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success = false", Long.class));
        assertEquals(2L, jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE checksum IS NOT NULL", Long.class));
    }

    @Test
    void businessKeysAreBoundAsValues() {
        String businessKey = "PR-' OR '1'='1";
        registration.register(new RegisterRequest(RequestId.newId(), businessKey, "Quotation test"));
        registration.register(new RegisterRequest(RequestId.newId(), "PR-OTHER", "Another request"));
        assertEquals(1, registration.countByBusinessKey(businessKey));
        assertEquals(0, registration.countByBusinessKey("PR-MISSING"));
    }

    private void insertRaw(String businessKey, String status, String title) {
        jdbc.update("INSERT INTO purchase_request (id, business_key, status, title) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), businessKey, status, title);
    }

    private void assertSqlState(String expectedState, org.junit.jupiter.api.function.Executable action) {
        DataIntegrityViolationException failure = assertThrows(DataIntegrityViolationException.class, action);
        SQLException sqlFailure = assertInstanceOf(SQLException.class, failure.getMostSpecificCause());
        assertEquals(expectedState, sqlFailure.getSQLState());
    }
}
