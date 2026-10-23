package edu.css3008.purchase.domain;

import java.util.Objects;
import java.util.UUID;

public record RequestId(UUID value) {
    public RequestId {
        Objects.requireNonNull(value, "Request ID must not be null");
    }

    public static RequestId newId() {
        return new RequestId(UUID.randomUUID());
    }

    public static RequestId parse(String text) {
        Objects.requireNonNull(text, "Request ID text must not be null");
        if (text.isBlank()) {
            throw new IllegalArgumentException("Request ID must not be blank");
        }
        UUID value = UUID.fromString(text);
        if (!value.toString().equalsIgnoreCase(text)) {
            throw new IllegalArgumentException("Request ID must be a canonical UUID");
        }
        return new RequestId(value);
    }
}

