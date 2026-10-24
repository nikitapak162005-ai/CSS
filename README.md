# Purchase Request Management System

CSS-3008 Corporate Development: one Java 21 project developed through three laboratories
and a later Midterm extension. The current version uses Spring Boot 3.4.5, PostgreSQL,
JPA/Hibernate, and Flyway. The original JDBC-only Lab 3 is preserved at Git tag `lab-3-jdbc`.

Repository: [nikitapak162005-ai/CSS](https://github.com/nikitapak162005-ai/CSS).

## Product

Employees create purchase requests, managers approve them, and procurement orders approved requests.
The business purpose is to prevent purchases from bypassing approval.

## Core item

A Purchase Request has a UUID identity, a unique human-readable business key such as `PR-19`,
a title, a creation time, and one of three statuses: `DRAFT`, `APPROVED`, `ORDERED`.
`PurchaseRequest.draft` creates a draft and rejects missing required values.

## Status table

| From     | To       | Allowed | Business reason                           |
|----------|----------|---------|-------------------------------------------|
| DRAFT    | APPROVED | true    | A manager approves the request            |
| APPROVED | ORDERED  | true    | An approved request can be ordered        |
| DRAFT    | ORDERED  | false   | A purchase cannot bypass approval         |
| ORDERED  | DRAFT    | false   | An ordered request cannot return to draft |

## Forbidden — why

DRAFT to ORDERED would spend company money before a manager approves it.
ORDERED to DRAFT would erase the meaning of a completed procurement decision.
Only the two forward transitions above are allowed. Repeating or reversing a status is rejected.
A procurement hold also blocks APPROVED to ORDERED; it does not block approval.

## Architecture

```text
purchase-request-management/       Maven parent
  domain/                         Plain Java business logic
    src/main/java/edu/css3008/purchase/domain/
    src/test/java/
  app/                            Spring Boot and infrastructure
    src/main/java/edu/css3008/purchase/
      config/                     Application and bean configuration
      service/                    Rule checks and transaction boundaries
      dto/                        RegisterRequest input
      client/                     ApprovalGateway outbound port
      handler/                    ApprovalHandler use case
      persistence/                RequestEntity, RequestJpa, status mapper
    src/main/resources/
      application.properties
      db/migration/               V1 and V2 SQL
    src/test/java/                Unit tests and PostgreSQL integration tests
  docs/VERIFICATION.md
  MIDTERM_DEFENSE.md
  PR_PROPOSAL.md
```

The course packages depend inward on the domain:

```text
app: dto      client      handler      config
      \          |          /            |
       \         |         /          service
        \        |        /              |
         \       |       /         persistence
          \      |      /                |
domain: RequestId, RequestStatus, RequestPolicy, RequestRule, RequestStore
```

`app` depends on `domain`. Domain never depends on app, Spring, JDBC, JPA, or Hibernate.
The domain POM has only a test-scoped JUnit dependency. Framework versions are managed by the
Spring Boot parent; dependency management does not add framework libraries to domain.

| Type | Responsibility |
|---|---|
| RequestId | Wrap a UUID and reject null or invalid identifier text |
| RequestStatus | Sealed interface with Draft, Approved, Ordered records |
| RequestPolicy | Return the destination status or reject a forbidden move |
| PurchaseRequest | Immutable request data with required-value validation |
| RequestRule | Small plain Java business-check contract |
| TransitionRule / BudgetHoldRule | Check transitions and the procurement hold |
| RuleChain | Run rules in order and stop at the first exception |
| RequestConfiguration | Assemble the chain with a Spring @Bean |
| RequestService | Receive RequestRule through constructor injection and check a move |
| RegisterRequest | Convert registration input into a validated draft |
| RequestStore | Plain Java persistence port |
| RegistrationService | Register requests, demonstrate rollback, and count requests |
| RequestEntity / RequestStatusMapper | Map the row and translate sealed statuses to/from text |
| RequestJpa | Insert with EntityManager.persist/flush and count with a named JPQL parameter |
| DuplicateRequest | Unchecked domain error retaining the original unique-violation cause |
| ApprovalGateway / ApprovalHandler | Outbound approval contract and IOException translation |

RequestService checks and returns a status. RegistrationService persists new drafts and counts
stored requests. The tests exercise these use cases directly. The application starts a non-web
Spring context, migrates and validates the database, and exits normally.

## Technologies

Java 21, Maven, Git, JUnit 5, Mockito, Spring Boot 3.4.5, Spring Data JPA, Hibernate, Flyway,
and PostgreSQL. The verified local database is PostgreSQL 17.11. Dependency versions for
Hibernate, Flyway, JUnit, and Mockito come from Spring Boot.

## PostgreSQL and running on this workstation

Portable tools and an isolated PostgreSQL cluster are installed outside the repository at
`$env:USERPROFILE\.codex\tools\css3008`. PostgreSQL listens only on `127.0.0.1:55432`.
Its generated password is outside Git and is loaded without printing it.

Run from the repository root in PowerShell:

```powershell
& "$env:USERPROFILE\.codex\tools\css3008\start-postgres.ps1"
. "$env:USERPROFILE\.codex\tools\css3008\env.ps1" -Database css3008_midterm
.\mvnw.cmd -q verify
.\mvnw.cmd -q install
. "$env:USERPROFILE\.codex\tools\css3008\env.ps1" -Database css3008_app
.\mvnw.cmd -pl app spring-boot:run
```

The first application startup applies both migrations to `css3008_app`. Later startups validate
migration checksums and the entity mapping. There is no browser page or HTTP server.
You can substitute `mvn` for `.\mvnw.cmd` after the environment script adds Maven to PATH.
On Unix, use `sh ./mvnw`. The wrapper downloads Maven 3.9.16 and checks its SHA-256 checksum.

To stop the local database:

```powershell
& "$env:USERPROFILE\.codex\tools\css3008\stop-postgres.ps1"
```

### Another workstation

Install Java 21 and PostgreSQL. The included Maven wrapper supplies Maven. Create a dedicated
local role and empty database from an administrator psql session:

```sql
CREATE ROLE css3008 LOGIN;
\password css3008
CREATE DATABASE css3008_midterm OWNER css3008;
```

`\password` prompts for a password instead of placing it in a script. Set these in PowerShell:

```powershell
$env:DB_URL = 'jdbc:postgresql://127.0.0.1:5432/css3008_midterm'
$env:DB_USERNAME = 'css3008'
$env:DB_PASSWORD = (Get-Credential -UserName css3008).GetNetworkCredential().Password
.\mvnw.cmd -q verify
.\mvnw.cmd -q install
.\mvnw.cmd -pl app spring-boot:run
```

The database owner needs permission to create schemas for integration tests. Do not point this
setup at an existing Lab 3 or shared database. The prepared Lab 3 database remains separate.
Docker is not required.

## Database schema and migrations

| Column | PostgreSQL type | Constraint or default |
|---|---|---|
| id | UUID | PRIMARY KEY, supplied by RequestId |
| business_key | TEXT | NOT NULL, UNIQUE, nonblank |
| status | TEXT | NOT NULL, CHECK DRAFT / APPROVED / ORDERED |
| title | TEXT | NOT NULL, nonblank |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |
| cost_center | TEXT | NOT NULL, DEFAULT 'UNASSIGNED' |

`V1__create_purchase_request.sql` is the unchanged original Lab 3 table creation SQL.
`V2__add_cost_center.sql` adds procurement metadata with a safe default for existing rows.
RequestEntity maps that column; newly mapped requests use UNASSIGNED as their initial value.
The domain creation timestamp is persisted; the SQL default also supports direct SQL inserts.

Flyway is the only active schema initializer. Basic SQL initialization is disabled.
Spring Boot runs Flyway before Hibernate validation. Configuration explicitly sets:

```properties
spring.sql.init.mode=never
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
spring.flyway.baseline-on-migrate=false
spring.flyway.out-of-order=false
spring.flyway.clean-disabled=true
spring.flyway.validate-migration-naming=true
```

Hibernate validates mapped tables and column types; it does not create or repair the schema.
Tests separately verify CHECK, UNIQUE, NOT NULL, defaults, and Flyway history.
Never edit an applied versioned migration. Add the next version so checksums and history remain
consistent. No Flyway clean or shared database reset is used.

The status CHECK validates stored values. RequestPolicy enforces transition history in Java;
a CHECK listing three values cannot enforce the previous status of a row.

## Tests and transaction outcomes

```powershell
mvn -q test
mvn -q verify
```

`test` runs domain and application unit tests without a database. `verify` additionally runs
real PostgreSQL tests through Maven Failsafe. It requires DB_URL, DB_USERNAME, and DB_PASSWORD;
missing settings fail the build with an explanation. Integration tests are never silently skipped.

Each run creates fresh schemas named `css3008_test_` followed by a random UUID without hyphens.
The test checks ownership by schema name before `DELETE FROM purchase_request`. It preserves
schema objects and flyway_schema_history, and leaves its schemas available for inspection.
It never drops tables or resets public. The Flyway upgrade test has its own fresh schema.

- `secondStatementRollsBack`: insertTwice inserts different UUIDs with the same key in one
  transaction. The second INSERT violates UNIQUE. DuplicateRequest escapes and both inserts
  roll back. The committed count is **0**.
- `secondRequestKeepsTheFirst`: each register call has its own transaction through Spring's
  service proxy. The first commits. The duplicate second call rolls back only itself.
  The committed count is **1**, and the first UUID remains.

The count-verification tests are not annotated with @Transactional. JPA flush forces the
constraint failure to occur inside the exception-translation boundary. Only SQLSTATE 23505
becomes DuplicateRequest; other persistence failures remain visible.

The four README rows remain in @CsvSource. Other tests cover domain invariants, rule ordering,
stop-on-failure, Spring wiring, status mapping, real schema constraints, UUID and timestamp
round trips, SQL parameter binding, and migration history. The V1-to-V2 upgrade test inserts
an existing row before V2 and proves that its data survives with the new default.

ApprovalHandler uses the real business rules and a mocked ApprovalGateway. Mockito verifies
the request ID and IOException-to-ApprovalUnavailable translation. No real HTTP call is made.
There is no live external adapter, so the handler is constructed explicitly rather than
registering a fake gateway bean.

See `docs/VERIFICATION.md` for actual executed commands and results.

## Development history

| Git tag | Completed stage |
|---|---|
| lab-1 | Plain Java 21 domain and 29 passing tests; no Spring or database |
| lab-2 | Spring Boot, constructor injection, SOLID rules and 39 passing tests; no database |
| lab-3-jdbc | Manual schema.sql, Spring JDBC, mocked port, PostgreSQL transactions |
| midterm-extension | JPA/Hibernate and Flyway V1/V2 after the verified JDBC stage |

The original Lab 3 assignment explicitly forbids JPA and Flyway. Lectures 6 and 7 introduce
them later. This repository preserves the JDBC milestone and labels ORM/migrations as the
Midterm extension. It does not claim that the original assignment required them.

`PR_PROPOSAL.md` contains a proposed Lab 2 title/checklist. Its CSS-18 identifier is illustrative,
not a real Jira ticket. The semester repository is linked above. No Jira ticket or pull request
has been created.
Supplied assignment files, slides, and screenshots remain local course reference materials
and are excluded from Git. Build outputs, IDE files, credentials, and local database data are
also excluded.

## Midterm demonstration

1. Read `MIDTERM_DEFENSE.md` and explain the product and four status rows.
2. Show RequestPolicy, RequestRule, RuleChain, and constructor injection.
3. Run `mvn -q verify` with the test database environment and explain counts 0 and 1.
4. Show RequestEntity, RequestJpa.persist/flush, and the two migrations.
5. Run the application, then inspect the separate demonstration database:

```powershell
. "$env:USERPROFILE\.codex\tools\css3008\env.ps1" -Database css3008_app
psql -X -c 'SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank'
psql -X -c '\d purchase_request'
git log --oneline --decorate
git show lab-3-jdbc:app/src/main/resources/db/schema.sql
git show lab-3-jdbc:app/src/main/java/edu/css3008/purchase/persistence/RequestJdbc.java
```

To run the historical JDBC version, use a clean working tree, `git switch --detach lab-3-jdbc`,
load the css3008_jdbc environment, and run verify. Its database already has the manually applied
schema. Return with `git switch main`. Merely inspecting git show does not change the checkout.

## Reference documentation

- [Spring Boot database initialization](https://docs.spring.io/spring-boot/3.4/how-to/data-initialization.html)
- [Spring transaction rollback](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/rolling-back.html)
- [PostgreSQL Windows binaries](https://www.postgresql.org/download/windows/)
