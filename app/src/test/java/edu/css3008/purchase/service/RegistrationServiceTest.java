package edu.css3008.purchase.service;

import edu.css3008.purchase.domain.RequestStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegistrationServiceTest {
    private final RequestStore store = mock(RequestStore.class);
    private final RegistrationService service = new RegistrationService(store);

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {" ", "   "})
    void rejectsBlankLookupKeysBeforeCallingPersistence(String businessKey) {
        assertThrows(IllegalArgumentException.class, () -> service.countByBusinessKey(businessKey));
        verifyNoInteractions(store);
    }

    @Test
    void rejectsNullLookupKeysBeforeCallingPersistence() {
        assertThrows(NullPointerException.class, () -> service.countByBusinessKey(null));
        verifyNoInteractions(store);
    }

    @Test
    void normalizesLookupKeysInTheSameWayAsRegistration() {
        when(store.countByBusinessKey("PR-19")).thenReturn(1L);
        assertEquals(1, service.countByBusinessKey(" PR-19 "));
        verify(store).countByBusinessKey("PR-19");
        verifyNoMoreInteractions(store);
    }
}
