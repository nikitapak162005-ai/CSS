package edu.css3008.purchase.persistence;

import edu.css3008.purchase.domain.DuplicateRequest;
import edu.css3008.purchase.domain.PurchaseRequest;
import edu.css3008.purchase.domain.RequestStore;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;

@Repository
public class RequestJpa implements RequestStore {
    private final EntityManager entityManager;

    public RequestJpa(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void insert(PurchaseRequest request) {
        try {
            entityManager.persist(RequestEntity.fromDomain(request));
            entityManager.flush();
        } catch (PersistenceException failure) {
            if (isUniqueViolation(failure)) {
                throw new DuplicateRequest(request.businessKey(), failure);
            }
            throw failure;
        }
    }

    @Override
    public long countByBusinessKey(String businessKey) {
        return entityManager.createQuery("""
                SELECT count(r) FROM RequestEntity r WHERE r.businessKey = :businessKey
                """, Long.class)
                .setParameter("businessKey", businessKey)
                .getSingleResult();
    }

    private boolean isUniqueViolation(Throwable failure) {
        Throwable cause = failure;
        while (cause != null) {
            if (cause instanceof SQLException sqlFailure && "23505".equals(sqlFailure.getSQLState())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
