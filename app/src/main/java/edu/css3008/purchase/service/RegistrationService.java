package edu.css3008.purchase.service;

import edu.css3008.purchase.domain.PurchaseRequest;
import edu.css3008.purchase.domain.RequestId;
import edu.css3008.purchase.domain.RequestStore;
import edu.css3008.purchase.dto.RegisterRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

@Service
public class RegistrationService {
    private final RequestStore store;

    public RegistrationService(RequestStore store) {
        this.store = store;
    }

    @Transactional
    public void register(RegisterRequest request) {
        store.insert(request.toDomain());
    }

    @Transactional
    public void insertTwice(String businessKey) {
        store.insert(PurchaseRequest.draft(RequestId.newId(), businessKey, "First purchase request"));
        store.insert(PurchaseRequest.draft(RequestId.newId(), businessKey, "Second purchase request"));
    }

    @Transactional(readOnly = true)
    public long countByBusinessKey(String businessKey) {
        Objects.requireNonNull(businessKey, "Business key must not be null");
        if (businessKey.isBlank()) {
            throw new IllegalArgumentException("Business key must not be blank");
        }
        return store.countByBusinessKey(businessKey.strip());
    }
}
