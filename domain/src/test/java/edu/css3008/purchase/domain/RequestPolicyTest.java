package edu.css3008.purchase.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class RequestPolicyTest {
    private final RequestPolicy policy = new RequestPolicy();

    @ParameterizedTest
    @CsvSource({
            "DRAFT, APPROVED, true",
            "APPROVED, ORDERED, true",
            "DRAFT, ORDERED, false",
            "ORDERED, DRAFT, false"
    })
    void enforcesTheReadmeTransitionTable(String fromCode, String toCode, boolean allowed) {
        RequestStatus from = RequestStatus.fromCode(fromCode);
        RequestStatus to = RequestStatus.fromCode(toCode);
        if (allowed) {
            assertSame(to, policy.move(from, to));
        } else {
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> policy.move(from, to));
            assertTrue(error.getMessage().contains(fromCode));
            assertTrue(error.getMessage().contains(toCode));
        }
    }

    @ParameterizedTest
    @CsvSource({"DRAFT,DRAFT", "APPROVED,APPROVED", "ORDERED,ORDERED", "APPROVED,DRAFT", "ORDERED,APPROVED"})
    void rejectsOtherTransitions(String from, String to) {
        assertThrows(IllegalStateException.class,
                () -> policy.move(RequestStatus.fromCode(from), RequestStatus.fromCode(to)));
    }

    @Test
    void rejectsNullStatuses() {
        assertThrows(NullPointerException.class, () -> policy.move(null, RequestStatus.APPROVED));
        assertThrows(NullPointerException.class, () -> policy.move(RequestStatus.DRAFT, null));
    }
}

