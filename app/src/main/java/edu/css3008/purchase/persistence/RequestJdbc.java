package edu.css3008.purchase.persistence;

import edu.css3008.purchase.domain.DuplicateRequest;
import edu.css3008.purchase.domain.PurchaseRequest;
import edu.css3008.purchase.domain.RequestStore;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Repository
public class RequestJdbc implements RequestStore {
    private final JdbcTemplate jdbc;

    public RequestJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insert(PurchaseRequest request) {
        try {
            jdbc.update("""
                    INSERT INTO purchase_request (id, business_key, status, title, created_at)
                    VALUES (?, ?, ?, ?, ?)
                    """, request.id().value(), request.businessKey(),
                    RequestStatusMapper.toDatabase(request.status()), request.title(),
                    OffsetDateTime.ofInstant(request.createdAt(), ZoneOffset.UTC));
        } catch (DuplicateKeyException failure) {
            throw new DuplicateRequest(request.businessKey(), failure);
        }
    }

    @Override
    public long countByBusinessKey(String businessKey) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM purchase_request WHERE business_key = ?", Long.class, businessKey);
    }
}
