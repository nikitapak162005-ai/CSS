# Midterm Defense

This guide supports a 10–15-minute demonstration of the same project developed in Labs 1, 2, and 3. The JPA/Hibernate and Flyway work is a later extension of the verified JDBC milestone.

## 1. Product and business rules — 2 minutes

**Question: What does your product do?**

It manages purchase requests. Employees create requests, managers approve them, and procurement orders approved requests.

**Question: Which item do you track?**

We track a Purchase Request. It has a UUID identity, a unique business key such as PR-19, a title, and a status.

**Question: Which transitions are allowed?**

DRAFT can become APPROVED, and APPROVED can become ORDERED. DRAFT cannot skip approval. ORDERED cannot return to DRAFT.

| From | To | Allowed | Reason |
|---|---|---|---|
| DRAFT | APPROVED | true | A manager approves the request |
| APPROVED | ORDERED | true | An approved request can be ordered |
| DRAFT | ORDERED | false | A purchase cannot bypass approval |
| ORDERED | DRAFT | false | An ordered request cannot return to draft |

**Question: Why do you use a sealed status type?**

A sealed type limits the possible status implementations. Our program has exactly three status types, and Java can check that a switch handles all of them.

**Question: What does RequestPolicy do?**

It enforces allowed transitions. It returns the destination status for a valid move and throws IllegalStateException for a forbidden move.

**Question: What happens to invalid input?**

Required values reject null or blank input. RequestId accepts a valid UUID. Unknown status text is rejected instead of becoming a new status.

## 2. Architecture and Spring — 2 minutes

**Question: Why are there two Maven modules?**

The domain module contains plain Java business rules. The app module contains Spring and database code. App depends on domain; domain does not depend on app or database libraries.

**Question: What is the difference between domain and persistence?**

Domain explains what a request means and which actions are allowed. Persistence explains how a row is stored. A database mapping should not decide a business transition.

**Question: How does Spring dependency injection work here?**

A configuration method creates a RequestRule chain as a bean. Spring creates RequestService and supplies that bean through its constructor. The service receives its dependency instead of creating it itself.

**Question: Why use a rule interface and a chain?**

Each rule has one check. TransitionRule checks the status policy. BudgetHoldRule blocks ordering while a procurement hold is active. RuleChain runs them in order and stops when a rule throws.

**Question: Does a procurement hold prevent approval?**

No. It prevents processing an order. A request can still move from DRAFT to APPROVED.

**Question: Where are the course packages?**

Domain contains business types. Config starts and assembles Spring. Service coordinates actions. Persistence stores rows. Client contains the outbound interface. DTO converts registration input, and handler coordinates approval. This project has no REST controller.

## 3. From JDBC to JPA — 2 minutes

**Question: What does JDBC do?**

JDBC lets Java execute SQL and read database results. Our original Lab 3 repository used Spring JdbcTemplate with SQL placeholders against PostgreSQL.

**Question: Can you show the original JDBC milestone?**

Yes. The Git tag lab-3-jdbc preserves that version. It uses schema.sql and JDBC, without JPA or Flyway. The current extension replaces the persistence implementation while keeping the business rules.

```powershell
git show lab-3-jdbc:app/src/main/resources/db/schema.sql
git ls-tree -r --name-only lab-3-jdbc
```

Use Git history to inspect this milestone without changing the current checkout.

**Question: What are JPA and Hibernate?**

JPA defines the persistence API and annotations. Hibernate implements that API. Our entity mapping tells Hibernate how Java fields correspond to PostgreSQL columns.

**Question: Why is RequestEntity separate from RequestStatus?**

RequestEntity represents the database row. RequestStatus is the sealed business type. A mapper converts the status to and from the database text and rejects unknown values.

**Question: Where does the UUID come from?**

RequestId creates it in Java. The entity uses that UUID directly, so it has no GeneratedValue annotation.

**Question: Why use EntityManager.persist rather than Spring Data save?**

This operation must insert a new row with an existing UUID. Persist states that intention directly. Save may use merge when an ID is already present and can perform an extra query.

**Question: Why do you flush?**

Persist can delay SQL until later. Flush sends pending writes to PostgreSQL while we can still translate a duplicate key failure into DuplicateRequest. Flush does not commit the transaction.

## 4. Schema and migrations — 2 minutes

**Question: Who creates the database tables?**

Flyway applies our versioned SQL migrations. Hibernate then validates the entity mapping against the resulting schema.

**Question: Why is ddl-auto set to validate?**

Validate checks that mapped columns and types match the database. It does not create or update tables. Reviewed migrations own schema changes.

**Question: Does Hibernate validate prove every database rule is correct?**

No. We also test PostgreSQL constraints directly. Validate does not prove business transitions, and extra unmapped database columns can exist.

**Question: Why is open-in-view disabled?**

Database work belongs within service operations. Keeping it disabled prevents a persistence session from being extended into a view layer. This project does not have a view layer.

**Question: What does V1 do?**

V1 creates purchase_request with a UUID primary key, a unique required business key, required status and title, a creation timestamp, and a CHECK for DRAFT, APPROVED, and ORDERED.

**Question: What does V2 do?**

V2 adds cost_center with the default UNASSIGNED. Existing rows receive a valid value, so adding a required column does not break them.

**Question: Why must an applied migration stay unchanged?**

Flyway records the migration version and checksum. Changing a successful file makes databases disagree with the repository. We add the next migration instead.

**Question: Why are baseline-on-migrate and out-of-order false?**

We want a fresh database to execute V1 and all later migrations in order. These settings prevent us from hiding an existing schema or an incorrect migration sequence.

**Question: How are database tests isolated?**

The test helper creates a new randomly named PostgreSQL schema. Flyway creates tables inside it. Tests delete only their request rows between cases while preserving the table and flyway_schema_history.

## 5. Transactions and outbound failures — 2 minutes

**Question: Why does secondStatementRollsBack produce count 0?**

Both inserts belong to one transaction. The second uses the same business key and fails. DuplicateRequest leaves the service method as a runtime exception, so the whole transaction rolls back.

**Question: Why does secondRequestKeepsTheFirst produce count 1?**

The first registration already committed in its own transaction. The second registration fails in another transaction. Rolling back the second transaction does not undo the first commit.

**Question: Why do the duplicate attempts use different UUIDs?**

We want to prove that the business key is unique. Different UUIDs ensure the failure is caused by the repeated business key rather than the primary key.

**Question: Why are the count tests not Transactional?**

They must observe the service's completed transaction outcomes. An extra transaction around the whole test would hide those boundaries.

**Question: Why is DuplicateRequest in domain?**

A caller should understand the business failure without depending on a PostgreSQL or Spring exception. It extends RuntimeException so it also preserves rollback behavior.

**Question: What does Mockito test?**

Mockito replaces ApprovalGateway with a test double. We verify the request passed to it and prove that an IOException becomes ApprovalUnavailable. The test makes no HTTP call, and the project does not include a live external approval adapter.

## 6. Demonstration — 2–3 minutes

1. Open README and show the four-row status table.
2. Open RequestStatus, RequestPolicy, and the parameterized test. Explain one allowed move and one forbidden move.
3. Show the rule chain and constructor injection. Point out that domain has no Spring, JDBC, or JPA imports.
4. Show RequestEntity and RequestJpa, then V1 and V2. Explain persist, flush, migrate, and validate.
5. Run verification and show the actual result. Find secondStatementRollsBack and secondRequestKeepsTheFirst in the integration-test report.
6. Start the application against a local PostgreSQL database. Show that migrations finish before Hibernate validation.
7. Show the Git milestones, including lab-3-jdbc.

Run from the project root in PowerShell:

```powershell
.\mvnw.cmd -q verify
.\mvnw.cmd -q install
.\mvnw.cmd -pl app spring-boot:run
git log --oneline --decorate
```

PostgreSQL must be running and the documented database environment variables must be set before the database demonstration. Use the exact setup commands in README. Verification is successful only when the commands actually finish successfully; this guide is not a test report.

**Question: What is the most important outcome of this project?**

The same business rules remain valid as the infrastructure changes. Tests prove the status rules and the two different transaction outcomes against PostgreSQL.
