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

