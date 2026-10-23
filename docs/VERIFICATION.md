# Verification record

These commands were executed on Java 21.0.11, Maven 3.9.16, and, for database stages,
real PostgreSQL 17.11 bound to 127.0.0.1:55432. No H2 or mocked database was used.

| Milestone | Command | Result |
|---|---|---|
| lab-1 | mvn -q test | Passed: 29 tests, no failures or skips |
| lab-2 | mvn -q verify | Passed: 39 tests, no failures or skips |
| lab-2 | mvn -q install; mvn -pl app spring-boot:run | Passed: Boot 3.4.5 started and exited normally |
| lab-3-jdbc | mvn -q verify | Passed: 48 unit tests and 9 PostgreSQL integration tests, no failures or skips |

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
