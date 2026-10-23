package edu.css3008.purchase.persistence;

import edu.css3008.purchase.domain.RequestStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class RequestStatusMapperTest {
    @ParameterizedTest
    @ValueSource(strings = {"DRAFT", "APPROVED", "ORDERED"})
    void statusRoundTripPreservesEachDomainStatus(String code) {
        RequestStatus status = RequestStatusMapper.fromDatabase(code);
        assertEquals(code, RequestStatusMapper.toDatabase(status));
    }

    @Test
    void unknownDatabaseStatusIsRejected() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> RequestStatusMapper.fromDatabase("CANCELLED"));
        assertTrue(failure.getMessage().contains("Unknown request status"));
    }

    @Test
    void nullDatabaseStatusIsRejected() {
        assertThrows(NullPointerException.class, () -> RequestStatusMapper.fromDatabase(null));
    }
}
