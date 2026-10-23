package edu.css3008.purchase.config;

import java.util.List;

import edu.css3008.purchase.domain.BudgetHoldRule;
import edu.css3008.purchase.domain.RequestPolicy;
import edu.css3008.purchase.domain.RequestRule;
import edu.css3008.purchase.domain.RuleChain;
import edu.css3008.purchase.domain.TransitionRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RequestConfiguration {
    @Bean
    public RequestRule requestRules() {
        return new RuleChain(List.of(
                new TransitionRule(new RequestPolicy()),
                new BudgetHoldRule()));
    }
}

