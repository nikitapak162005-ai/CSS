package edu.css3008.purchase.domain;

public interface RequestStore {
    void insert(PurchaseRequest request);

    long countByBusinessKey(String businessKey);
}
