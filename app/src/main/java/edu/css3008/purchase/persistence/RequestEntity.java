package edu.css3008.purchase.persistence;

import edu.css3008.purchase.domain.PurchaseRequest;
import edu.css3008.purchase.domain.RequestId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "purchase_request")
public class RequestEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "business_key", nullable = false, unique = true, columnDefinition = "text")
    private String businessKey;

    @Column(name = "status", nullable = false, columnDefinition = "text")
    private String status;

    @Column(name = "title", nullable = false, columnDefinition = "text")
    private String title;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "timestamp with time zone")
    private Instant createdAt;

    @Column(name = "cost_center", nullable = false, columnDefinition = "text")
    private String costCenter;

    protected RequestEntity() {}

    public static RequestEntity fromDomain(PurchaseRequest request) {
        Objects.requireNonNull(request, "Purchase request must not be null");
        RequestEntity entity = new RequestEntity();
        entity.id = request.id().value();
        entity.businessKey = request.businessKey();
        entity.status = RequestStatusMapper.toDatabase(request.status());
        entity.title = request.title();
        entity.createdAt = request.createdAt();
        entity.costCenter = "UNASSIGNED";
        return entity;
    }

    public PurchaseRequest toDomain() {
        return new PurchaseRequest(new RequestId(id), businessKey,
                RequestStatusMapper.fromDatabase(status), title, createdAt);
    }

    public String getCostCenter() {
        return costCenter;
    }
}
