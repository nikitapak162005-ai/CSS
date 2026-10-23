package edu.css3008.purchase.domain;

@FunctionalInterface
public interface RequestRule {
    void check(RequestStatus from, RequestStatus to, boolean procurementHold);
}

