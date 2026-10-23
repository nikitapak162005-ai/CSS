package edu.css3008.purchase.service;

import edu.css3008.purchase.config.RequestConfiguration;
import edu.css3008.purchase.domain.RequestStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(classes = {RequestConfiguration.class, RequestService.class})
class RequestServiceTest {
    @Autowired
    private RequestService service;

    @Test
    void springInjectsTheRuleChain() {
        assertSame(RequestStatus.APPROVED,
                service.move(RequestStatus.DRAFT, RequestStatus.APPROVED, false));
    }

    @Test
    void injectedTransitionRuleRejectsBypassedApproval() {
        assertThrows(IllegalStateException.class,
                () -> service.move(RequestStatus.DRAFT, RequestStatus.ORDERED, false));
    }

    @Test
    void injectedHoldRuleBlocksOrdering() {
        assertThrows(IllegalStateException.class,
                () -> service.move(RequestStatus.APPROVED, RequestStatus.ORDERED, true));
    }
}

