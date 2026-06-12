package io.github.mavencrafted.scheduling.audit.demo.scheduler;

import io.github.mavencrafted.scheduling.audit.ScheduledAudit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class InvoiceSynchronizationScheduler {

    private static final long INITIAL_DELAY_MILLIS = 10_000;
    private static final long FAILURE_INTERVAL_MILLIS = 120_000;

    @Scheduled(initialDelay = INITIAL_DELAY_MILLIS, fixedDelay = FAILURE_INTERVAL_MILLIS)
    @ScheduledAudit(
            schedulerId = "invoice-sync",
            tags = {"billing", "integration"}
    )
    void synchronizeInvoices() {
        // Demonstrates FAILED audit events while preserving normal Spring scheduling behavior.
        throw new IllegalStateException("Failed to connect to billing service");
    }
}
