package edu.css3008.purchase.handler;

import java.io.IOException;
import java.util.Objects;

import edu.css3008.purchase.client.ApprovalGateway;
import edu.css3008.purchase.domain.ApprovalUnavailable;
import edu.css3008.purchase.domain.RequestId;
import edu.css3008.purchase.domain.RequestStatus;
import edu.css3008.purchase.service.RequestService;

public final class ApprovalHandler {
    private final RequestService requests;
    private final ApprovalGateway gateway;

    public ApprovalHandler(RequestService requests, ApprovalGateway gateway) {
        this.requests = Objects.requireNonNull(requests, "Request service must not be null");
        this.gateway = Objects.requireNonNull(gateway, "Approval gateway must not be null");
    }

    public RequestStatus approve(RequestId id, RequestStatus from, boolean procurementHold) {
        Objects.requireNonNull(id, "Request ID must not be null");
        RequestStatus approved = requests.move(from, RequestStatus.APPROVED, procurementHold);
        try {
            gateway.requestApproval(id);
        } catch (IOException exception) {
            throw new ApprovalUnavailable(id, exception);
        }
        return approved;
    }
}

