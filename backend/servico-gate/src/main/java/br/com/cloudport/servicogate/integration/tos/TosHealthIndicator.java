package br.com.cloudport.servicogate.integration.tos;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

@Component
public class TosHealthIndicator extends AbstractHealthIndicator {

    private final CircuitBreaker circuitBreaker;

    public TosHealthIndicator(@Qualifier("tosCircuitBreaker") CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        CircuitBreaker.State state = circuitBreaker.getState();
        builder.withDetail("state", state.name())
                .withDetail("failureRate", circuitBreaker.getMetrics().getFailureRate())
                .withDetail("slowCallRate", circuitBreaker.getMetrics().getSlowCallRate());
        if (state == CircuitBreaker.State.CLOSED || state == CircuitBreaker.State.HALF_OPEN) {
            builder.up();
        } else {
            builder.down();
        }
    }
}
