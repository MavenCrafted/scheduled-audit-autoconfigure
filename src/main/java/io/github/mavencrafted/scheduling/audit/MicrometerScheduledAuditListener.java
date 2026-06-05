package io.github.mavencrafted.scheduling.audit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.springframework.util.StringUtils;

/**
 * {@link ScheduledAuditListener} that publishes metrics for scheduled audit events
 * to a Micrometer {@link io.micrometer.core.instrument.MeterRegistry}.
 */
final class MicrometerScheduledAuditListener implements ScheduledAuditListener {

    private static final String EXECUTIONS_METRIC = "scheduled.audit.executions";
    private static final String EXECUTIONS_METRIC_DESCRIPTION = "Total number of scheduled task executions grouped by execution status";

    private static final String DURATION_METRIC = "scheduled.audit.duration";
    private static final String DURATION_METRIC_DESCRIPTION = "Execution duration of scheduled tasks grouped by execution status";

    private static final String SCHEDULER_ID_TAG = "scheduler.id";
    private static final String STATUS_TAG = "status";

    private final MeterRegistry registry;

    MicrometerScheduledAuditListener(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void onEvent(ScheduledAuditEvent event) {
        if (!isCompleted(event) || !hasSchedulerId(event)) {
            return;
        }

        Tags tags = Tags.of(
                SCHEDULER_ID_TAG, event.getSchedulerId(),
                STATUS_TAG, event.getStatus().name()
        );

        Counter.builder(EXECUTIONS_METRIC)
                .description(EXECUTIONS_METRIC_DESCRIPTION)
                .tags(tags)
                .register(registry)
                .increment();

        if (event.getDuration() != null) {
            Timer.builder(DURATION_METRIC)
                    .description(DURATION_METRIC_DESCRIPTION)
                    .tags(tags)
                    .register(registry)
                    .record(event.getDuration());
        }
    }

    private boolean isCompleted(ScheduledAuditEvent event) {
        return switch (event.getStatus()) {
            case SUCCEEDED, FAILED -> true;
            default -> false;
        };
    }

    private boolean hasSchedulerId(ScheduledAuditEvent event) {
        return StringUtils.hasText(event.getSchedulerId());
    }
}
