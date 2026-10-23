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

