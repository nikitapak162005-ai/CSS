package edu.css3008.purchase.client;

import java.io.IOException;

import edu.css3008.purchase.domain.RequestId;

@FunctionalInterface
public interface ApprovalGateway {
    void requestApproval(RequestId id) throws IOException;
}

