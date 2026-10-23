# Purchase Request Management System

## Product

Employees create purchase requests, managers approve them, and procurement orders approved requests.

## Core item

A Purchase Request has a UUID identity, a unique human-readable business key such as PR-19,
a title, a creation time, and one of three sealed statuses: DRAFT, APPROVED, ORDERED.

## Status table

| From | To | Allowed | Business reason |
|---|---|---|---|
| DRAFT | APPROVED | true | A manager approves the request |
| APPROVED | ORDERED | true | An approved request can be ordered |
| DRAFT | ORDERED | false | A purchase cannot bypass approval |
| ORDERED | DRAFT | false | An ordered request cannot return to draft |

## Forbidden — why

DRAFT to ORDERED would spend company money before a manager approves it.
ORDERED to DRAFT would erase the meaning of a completed procurement decision.
Only the two forward transitions above are allowed. Repeating or reversing a status is rejected.

## Lab 1

Java 21 and Maven are required. Run from the repository root:

```powershell
mvn -q test
```

The root Maven project contains the pure Java domain module. RequestId validates UUID identity;
RequestStatus is a sealed interface with three record implementations; RequestPolicy returns the
destination status or throws IllegalStateException. PurchaseRequest validates required text and
starts in DRAFT. There is no Spring or database in this milestone.

Supplied lecture slides and assignment screenshots remain local course reference materials and
are excluded from version control.

## Architecture

The Maven modules are domain (plain Java) and app (Spring Boot). Dependencies point inward:

```text
app: dto        client        handler        config
     input      outbound      use cases      Application, @Bean
          \        |             /                |
           \       |            /          service.RequestService
            \      |           /                  |
domain: RequestId, RequestStatus, RequestPolicy, RequestRule
        TransitionRule, BudgetHoldRule, RuleChain, PurchaseRequest
```

The client, dto, and handler packages are reserved boundaries and contain no fake implementations.
The domain module has only JUnit as a test dependency. It never imports Spring or database code.
RequestService uses constructor injection. RequestConfiguration creates a chain that checks the
status transition first and the procurement hold second. A thrown exception stops the chain.
A hold blocks ordering but allows approval.

## Lab 2 and running the application

Spring Boot 3.4.5 runs the same Lab 1 domain. There is no HTTP server or database in this milestone.
Application is in config and explicitly scans the complete product package.

```powershell
mvn -q verify
mvn -q install
mvn -pl app spring-boot:run
```

The non-web application starts its Spring context and then exits normally. The automated wiring
test calls the injected RequestService. On Windows without Maven installed, use .\\mvnw.cmd
instead of mvn. On Unix, use sh ./mvnw. The wrapper downloads Maven and verifies its checksum.
See PR_PROPOSAL.md for the proposed Lab 2 title and checklist; no remote PR is claimed.

## Lab 3: PostgreSQL and JDBC

Lab 3 adds Spring JDBC and the PostgreSQL driver. It contains no ORM or migration framework.
RequestJdbc implements the plain Java RequestStore port. It inserts requests and counts rows
by business key using bound SQL placeholders. RequestStatusMapper rejects unknown status text.
RegistrationService owns transactions; DuplicateRequest is an unchecked domain exception.

The hand-written table is in app/src/main/resources/db/schema.sql:

| Column | PostgreSQL type | Constraint |
|---|---|---|
| id | UUID | PRIMARY KEY, supplied by RequestId |
| business_key | TEXT | NOT NULL, UNIQUE |
| status | TEXT | NOT NULL, CHECK DRAFT / APPROVED / ORDERED |
| title | TEXT | NOT NULL |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |

The CHECK validates stored status values. RequestPolicy enforces allowed transitions in Java;
the CHECK alone does not enforce the transition history. Blank text is rejected in Java.

### Local PostgreSQL on this workstation

Portable Maven and PostgreSQL were installed outside the repository. The cluster listens only
on 127.0.0.1:55432. Its generated password is outside Git and is loaded without printing it.

```powershell
& "$env:USERPROFILE\.codex\tools\css3008\start-postgres.ps1"
. "$env:USERPROFILE\.codex\tools\css3008\env.ps1" -Database css3008_jdbc
psql -X -v ON_ERROR_STOP=1 -f app/src/main/resources/db/schema.sql
mvn -q verify
mvn -q install
mvn -pl app spring-boot:run
```

Apply schema.sql only once to the new Lab 3 database. Automatic production SQL initialization
is disabled. For another machine, install Java 21 and PostgreSQL, create a dedicated local role
and database, and set DB_URL, DB_USERNAME, DB_PASSWORD. Use Maven or the included Maven wrapper.

### Integration test isolation

The verify phase always runs the PostgreSQL integration tests through Maven Failsafe. They do
not use H2 and are not silently skipped if connection settings are missing. Each test run creates
a new randomly named schema, applies schema.sql there, and deletes rows only inside that owned
schema before each test. It leaves the schema available for inspection. It never resets public
or a shared table. Database settings come from the three DB_ environment variables above.

### Two transaction outcomes

secondStatementRollsBack calls insertTwice: two different UUIDs share one business key inside
one transaction. The second INSERT fails, DuplicateRequest escapes, and both inserts roll back:
count = 0.

secondRequestKeepsTheFirst calls register twice, each time through the Spring service proxy.
The first call commits. The second call fails and rolls back only its own transaction: count = 1.
The verification tests themselves are not transactional, so they observe the completed outcomes.

### Mocked outbound port

ApprovalHandler checks the transition using the real rules, then calls ApprovalGateway with the
request ID. An IOException becomes ApprovalUnavailable with its original cause. Mockito verifies
the ID and error translation. There is no HTTP request or fake live approval adapter. The handler
can be constructed with a real adapter when one exists; it is not auto-registered without one.

