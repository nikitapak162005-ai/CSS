package edu.css3008.purchase.domain;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class RequestInvariantTest {
    @Test
    void rejectsNullUuid() {
        assertThrows(NullPointerException.class, () -> new RequestId(null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "not-a-uuid", "1-1-1-1-1"})
    void rejectsInvalidIdText(String value) {
        assertThrows(RuntimeException.class, () -> RequestId.parse(value));
    }

    @Test
    void preservesUuidIdentity() {
        UUID uuid = UUID.randomUUID();
        assertEquals(new RequestId(uuid), RequestId.parse(uuid.toString()));
        assertNotEquals(RequestId.newId(), RequestId.newId());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  "})
    void rejectsMissingBusinessKeyAndTitle(String value) {
        assertThrows(RuntimeException.class,
                () -> PurchaseRequest.draft(RequestId.newId(), value, "Office chairs"));
        assertThrows(RuntimeException.class,
                () -> PurchaseRequest.draft(RequestId.newId(), "PR-19", value));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"UNKNOWN", "draft", " "})
    void rejectsUnknownStatus(String value) {
        assertThrows(RuntimeException.class, () -> RequestStatus.fromCode(value));
    }

    @Test
    void rejectsMissingRequestParts() {
        assertThrows(NullPointerException.class, () -> PurchaseRequest.draft(null, "PR-19", "Chairs"));
        assertThrows(NullPointerException.class, () -> new PurchaseRequest(
                RequestId.newId(), "PR-19", null, "Chairs", Instant.now()));
        assertThrows(NullPointerException.class, () -> new PurchaseRequest(
                RequestId.newId(), "PR-19", RequestStatus.DRAFT, "Chairs", null));
    }

    @Test
    void createsDraftAndNormalizesRequiredText() {
        PurchaseRequest request = PurchaseRequest.draft(RequestId.newId(), " PR-19 ", " Office chairs ");
        assertEquals("PR-19", request.businessKey());
        assertEquals("Office chairs", request.title());
        assertEquals(RequestStatus.DRAFT, request.status());
        assertNotNull(request.createdAt());
    }
}

