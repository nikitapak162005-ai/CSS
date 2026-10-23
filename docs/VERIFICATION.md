# Verification record

These commands were executed on Java 21.0.11, Maven 3.9.16, and, for database stages,
real PostgreSQL 17.11 bound to 127.0.0.1:55432. No H2 or mocked database was used.

| Milestone | Command | Result |
|---|---|---|
| lab-1 | mvn -q test | Passed: 29 tests, no failures or skips |
| lab-2 | mvn -q verify | Passed: 39 tests, no failures or skips |
| lab-2 | mvn -q install; mvn -pl app spring-boot:run | Passed: Boot 3.4.5 started and exited normally |
| lab-3-jdbc | mvn -q verify | Passed: 48 unit tests and 9 PostgreSQL integration tests, no failures or skips |
| midterm-extension | mvn -q clean verify | Passed: initial JPA/Flyway verification from fresh test schemas |
| midterm-extension | mvn -q verify | Passed: final 53 unit tests and 11 PostgreSQL integration tests, no failures or skips |
| midterm-extension | java -jar app/target/app-1.0.0-SNAPSHOT.jar | Passed: startup against an empty css3008_app database, both migrations applied before Hibernate validation |

Each database test run creates a fresh schema named css3008_test_ followed by a random UUID
without hyphens. Tests delete only rows in that schema. Schemas remain available for inspection.
The environment variables DB_URL, DB_USERNAME, and DB_PASSWORD are required for verify.

The two required database tests assert completed transaction outcomes from outside the tested
transactions: secondStatementRollsBack = 0, secondRequestKeepsTheFirst = 1.

The Lab 3 schema was also applied manually with psql to the dedicated css3008_jdbc database.
Two SQL demonstrations produced genuine PostgreSQL unique violations (SQLSTATE 23505):
an explicit rollback left 0 rows; a failed second transaction left the first committed row.
The manual demonstration row remains in that separate JDBC database.

The Maven wrapper was also executed with .\mvnw.cmd --version. It downloaded and verified
Maven 3.9.16 using the committed SHA-256 checksum.

## Midterm evidence

- A psql query confirmed that css3008_app/public contained 0 tables before application startup.
- Flyway applied V1 and V2; Hibernate 6.6.13.Final initialized successfully with ddl-auto=validate.
- A subsequent psql query showed successful migration history versions 1 and 2 and all six
  purchase_request columns, including cost_center with default UNASSIGNED.
- The separate upgrade test applied V1, inserted a legacy request, then applied V2. The title
  and UUID survived, the new field received UNASSIGNED, and another migrate applied 0 changes.
- V1 and the schema.sql blob at lab-3-jdbc have the same Git object hash:
  7bc6971b123c5ed96110dfcf2888a04fc0e60168.
- The dedicated Lab 3 database still contains the first manually committed demonstration row.
- Application startup sets the English locale; test JVMs start with English language settings,
  including PostgreSQL JDBC diagnostic labels. The local server also uses lc_messages=C.
- Expected unique-violation messages in the transaction tests have SQLSTATE 23505. Mockito emits
  a Java 21 agent-attachment warning; it does not prevent any test from executing.

No remote repository, pull request, Docker application image, REST API, or external HTTP adapter
was created. No environmental blocker remained after installing the portable local tools.
