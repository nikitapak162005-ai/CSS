package edu.css3008.purchase.domain;

import java.util.List;

public final class RuleChain implements RequestRule {
    private final List<RequestRule> rules;

    public RuleChain(List<RequestRule> rules) {
        this.rules = List.copyOf(rules);
    }

    @Override
    public void check(RequestStatus from, RequestStatus to, boolean procurementHold) {
        for (RequestRule rule : rules) {
            rule.check(from, to, procurementHold);
        }
    }
}

