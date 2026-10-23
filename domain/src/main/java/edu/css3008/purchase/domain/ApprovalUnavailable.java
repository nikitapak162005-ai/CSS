package edu.css3008.purchase.domain;

public final class ApprovalUnavailable extends RuntimeException {
    public ApprovalUnavailable(RequestId id, Throwable cause) {
        super("The approval system is unavailable for request " + id.value(), cause);
    }
}

