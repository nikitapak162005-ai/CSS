package edu.css3008.purchase.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuleChainTest {
    private final RequestRule rules = new RuleChain(List.of(
            new TransitionRule(new RequestPolicy()), new BudgetHoldRule()));

    @Test
    void stopsAtTheFirstFailure() {
        AtomicBoolean nextRuleCalled = new AtomicBoolean();
        IllegalStateException failure = new IllegalStateException("First rule failed");
        RequestRule first = (from, to, hold) -> { throw failure; };
        RequestRule second = (from, to, hold) -> nextRuleCalled.set(true);
        RuleChain chain = new RuleChain(List.of(first, second));

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> chain.check(RequestStatus.DRAFT, RequestStatus.APPROVED, false)));
        assertFalse(nextRuleCalled.get());
    }

    @Test
    void executesRulesInOrder() {
        List<Integer> calls = new ArrayList<>();
        RuleChain chain = new RuleChain(List.of(
                (from, to, hold) -> calls.add(1),
                (from, to, hold) -> calls.add(2)));
        chain.check(RequestStatus.DRAFT, RequestStatus.APPROVED, false);
        assertEquals(List.of(1, 2), calls);
    }

    @Test
    void takesAnImmutableCopyOfTheRules() {
        List<RequestRule> source = new ArrayList<>();
        source.add(new TransitionRule(new RequestPolicy()));
        RuleChain chain = new RuleChain(source);
        source.clear();
        assertThrows(IllegalStateException.class,
                () -> chain.check(RequestStatus.DRAFT, RequestStatus.ORDERED, false));
    }

    @Test
    void blocksOrderingDuringProcurementHold() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> rules.check(RequestStatus.APPROVED, RequestStatus.ORDERED, true));
        assertTrue(error.getMessage().contains("procurement hold"));
    }

    @Test
    void allowsApprovalDuringProcurementHold() {
        assertDoesNotThrow(() -> rules.check(RequestStatus.DRAFT, RequestStatus.APPROVED, true));
    }

    @Test
    void allowsOrderingWithoutHold() {
        assertDoesNotThrow(() -> rules.check(RequestStatus.APPROVED, RequestStatus.ORDERED, false));
    }

    @Test
    void enforcesTransitionsEvenWithoutHold() {
        assertThrows(IllegalStateException.class,
                () -> rules.check(RequestStatus.DRAFT, RequestStatus.ORDERED, false));
    }
}

