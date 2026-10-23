package edu.css3008.purchase.handler;

import java.io.IOException;

import edu.css3008.purchase.client.ApprovalGateway;
import edu.css3008.purchase.config.RequestConfiguration;
import edu.css3008.purchase.domain.ApprovalUnavailable;
import edu.css3008.purchase.domain.RequestId;
import edu.css3008.purchase.domain.RequestStatus;
import edu.css3008.purchase.service.RequestService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApprovalHandlerTest {
    private final ApprovalGateway gateway = mock(ApprovalGateway.class);
    private final RequestService requests = new RequestService(new RequestConfiguration().requestRules());
    private final ApprovalHandler handler = new ApprovalHandler(requests, gateway);

    @Test
    void sendsTheCorrectIdToTheApprovalPort() throws IOException {
        RequestId id = RequestId.newId();
        assertEquals(RequestStatus.APPROVED, handler.approve(id, RequestStatus.DRAFT, false));
        verify(gateway).requestApproval(id);
        verifyNoMoreInteractions(gateway);
    }

    @Test
    void translatesAnUnavailableDownstreamSystem() throws IOException {
        RequestId id = RequestId.newId();
        IOException downstreamFailure = new IOException("Approval system connection refused");
        doThrow(downstreamFailure).when(gateway).requestApproval(id);

        ApprovalUnavailable error = assertThrows(ApprovalUnavailable.class,
                () -> handler.approve(id, RequestStatus.DRAFT, false));

        assertSame(downstreamFailure, error.getCause());
        assertTrue(error.getMessage().contains(id.value().toString()));
        verify(gateway).requestApproval(id);
        verifyNoMoreInteractions(gateway);
    }

    @Test
    void rejectsAnInvalidTransitionBeforeCallingThePort() {
        assertThrows(IllegalStateException.class,
                () -> handler.approve(RequestId.newId(), RequestStatus.ORDERED, false));
        verifyNoInteractions(gateway);
    }

    @Test
    void rejectsMissingIdentityBeforeCallingThePort() {
        assertThrows(NullPointerException.class,
                () -> handler.approve(null, RequestStatus.DRAFT, false));
        verifyNoInteractions(gateway);
    }
}

