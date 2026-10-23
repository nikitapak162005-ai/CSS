package edu.css3008.purchase.domain;

import java.util.Objects;

public final class TransitionRule implements RequestRule {
    private final RequestPolicy policy;

    public TransitionRule(RequestPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "Request policy must not be null");
    }

    @Override
    public void check(RequestStatus from, RequestStatus to, boolean procurementHold) {
        policy.move(from, to);
    }
}

