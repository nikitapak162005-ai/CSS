package edu.css3008.purchase.domain;

public final class BudgetHoldRule implements RequestRule {
    @Override
    public void check(RequestStatus from, RequestStatus to, boolean procurementHold) {
        if (procurementHold && to instanceof RequestStatus.Ordered) {
            throw new IllegalStateException("Cannot order a purchase request while a procurement hold is active");
        }
    }
}

