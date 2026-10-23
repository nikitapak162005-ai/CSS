package edu.css3008.purchase.domain;

import java.util.Objects;

public final class RequestPolicy {
    public RequestStatus move(RequestStatus from, RequestStatus to) {
        Objects.requireNonNull(from, "Source status must not be null");
        Objects.requireNonNull(to, "Destination status must not be null");
        boolean allowed = switch (from) {
            case RequestStatus.Draft ignored -> to instanceof RequestStatus.Approved;
            case RequestStatus.Approved ignored -> to instanceof RequestStatus.Ordered;
            case RequestStatus.Ordered ignored -> false;
        };
        if (!allowed) {
            throw new IllegalStateException("Cannot move a purchase request from "
                    + from.code() + " to " + to.code());
        }
        return to;
    }
}

