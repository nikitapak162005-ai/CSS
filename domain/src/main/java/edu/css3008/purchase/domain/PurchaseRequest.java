package edu.css3008.purchase.domain;

import java.time.Instant;
import java.util.Objects;

public record PurchaseRequest(RequestId id, String businessKey, RequestStatus status,
                              String title, Instant createdAt) {
    public PurchaseRequest {
        Objects.requireNonNull(id, "Request ID must not be null");
        Objects.requireNonNull(status, "Request status must not be null");
        Objects.requireNonNull(createdAt, "Creation time must not be null");
        businessKey = requiredText(businessKey, "Business key");
        title = requiredText(title, "Title");
    }

    public static PurchaseRequest draft(RequestId id, String businessKey, String title) {
        return new PurchaseRequest(id, businessKey, RequestStatus.DRAFT, title, Instant.now());
    }

    private static String requiredText(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.strip();
    }
}

