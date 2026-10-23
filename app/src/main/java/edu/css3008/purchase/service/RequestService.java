package edu.css3008.purchase.service;

import edu.css3008.purchase.domain.RequestRule;
import edu.css3008.purchase.domain.RequestStatus;
import org.springframework.stereotype.Service;

@Service
public class RequestService {
    private final RequestRule rules;

    public RequestService(RequestRule rules) {
        this.rules = rules;
    }

    public RequestStatus move(RequestStatus from, RequestStatus to, boolean procurementHold) {
        rules.check(from, to, procurementHold);
        return to;
    }
}

