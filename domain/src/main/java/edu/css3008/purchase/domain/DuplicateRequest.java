package edu.css3008.purchase.domain;

public class DuplicateRequest extends RuntimeException {
    public DuplicateRequest(String businessKey, Throwable cause) {
        super("A purchase request with this business key or ID already exists: " + businessKey, cause);
    }
}
