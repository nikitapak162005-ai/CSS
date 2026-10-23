package edu.css3008.purchase.persistence;

import edu.css3008.purchase.domain.RequestStatus;

import java.util.Objects;

public final class RequestStatusMapper {
    private RequestStatusMapper() {}

    public static String toDatabase(RequestStatus status) {
        return Objects.requireNonNull(status, "Request status must not be null").code();
    }

    public static RequestStatus fromDatabase(String status) {
        return RequestStatus.fromCode(status);
    }
}
