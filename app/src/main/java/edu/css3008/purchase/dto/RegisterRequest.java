package edu.css3008.purchase.dto;

import edu.css3008.purchase.domain.PurchaseRequest;
import edu.css3008.purchase.domain.RequestId;

public record RegisterRequest(RequestId id, String businessKey, String title) {
    public PurchaseRequest toDomain() {
        return PurchaseRequest.draft(id, businessKey, title);
    }
}
