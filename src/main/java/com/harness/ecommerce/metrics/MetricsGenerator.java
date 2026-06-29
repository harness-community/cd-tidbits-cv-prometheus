package com.harness.ecommerce.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * MetricsGenerator — emits a Prometheus counter at different rates
 * depending on the APP_MODE environment variable.
 *
 * stable   → increments by 1 every second  (~1/sec)
 *            rate(ecommerce_requests_total[5m]) stays well below threshold → CV passes
 *
 * unstable → increments by 50–100 every second
 *            rate(ecommerce_requests_total[5m]) spikes far above threshold → CV fails
 *
 * Prometheus scrapes this metric at /actuator/prometheus
 * Harness CV evaluates: rate(ecommerce_requests_total{namespace="cv-staging"}[5m])
 */
@Component
public class MetricsGenerator {

    private final Counter requestCounter;
    private final String appMode;

    public MetricsGenerator(MeterRegistry registry) {
        this.requestCounter = Counter.builder("ecommerce_requests_total")
                .description("Total number of requests processed by the ecommerce app")
                .register(registry);
        this.appMode = System.getenv().getOrDefault("APP_MODE", "stable");
        System.out.println("MetricsGenerator started in [" + appMode + "] mode");
    }

    @Scheduled(fixedRate = 1000)
    public void generateMetrics() {
        if ("unstable".equals(appMode)) {
            // Simulate a regression — massive request spike
            // rate(ecommerce_requests_total[5m]) ≈ 50–100/sec → far above threshold
            int spike = 50 + (int) (Math.random() * 51);
            requestCounter.increment(spike);
        } else {
            // Normal stable behaviour
            // rate(ecommerce_requests_total[5m]) ≈ 1/sec → well below threshold
            requestCounter.increment();
        }
    }
}
