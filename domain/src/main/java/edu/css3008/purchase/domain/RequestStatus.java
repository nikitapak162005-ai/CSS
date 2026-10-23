package edu.css3008.purchase.domain;

import java.util.Objects;

public sealed interface RequestStatus
        permits RequestStatus.Draft, RequestStatus.Approved, RequestStatus.Ordered {
    RequestStatus DRAFT = new Draft();
    RequestStatus APPROVED = new Approved();
    RequestStatus ORDERED = new Ordered();

    record Draft() implements RequestStatus {}
    record Approved() implements RequestStatus {}
    record Ordered() implements RequestStatus {}

    default String code() {
        return switch (this) {
            case Draft ignored -> "DRAFT";
            case Approved ignored -> "APPROVED";
            case Ordered ignored -> "ORDERED";
        };
    }

    static RequestStatus fromCode(String code) {
        Objects.requireNonNull(code, "Request status must not be null");
        return switch (code) {
            case "DRAFT" -> DRAFT;
            case "APPROVED" -> APPROVED;
            case "ORDERED" -> ORDERED;
            default -> throw new IllegalArgumentException("Unknown request status: " + code);
        };
    }
}

