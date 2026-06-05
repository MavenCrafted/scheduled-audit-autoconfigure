package io.github.mavencrafted.scheduling.audit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class MicrometerScheduledAuditListenerTests {

    private static final Instant STARTED_AT = Instant.parse("2026-01-01T10:00:00Z");

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final MicrometerScheduledAuditListener listener =
            new MicrometerScheduledAuditListener(registry);

    @Test
    void incrementsExecutionCounterForSucceededEvent() {
        ScheduledAuditEvent event = event("test-scheduler", STARTED_AT.plusMillis(100));

        listener.onEvent(event);

        Counter counter = registry.find("mavencrafted.scheduled.audit.executions")
                .tag("scheduler.id", "test-scheduler")
                .tag("status", "SUCCEEDED")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void recordsDurationWhenStartedAtAndFinishedAtArePresent() {
        ScheduledAuditEvent event = event("test-scheduler", STARTED_AT.plusMillis(250));

        listener.onEvent(event);

        Timer timer = registry.find("mavencrafted.scheduled.audit.duration")
                .tag("scheduler.id", "test-scheduler")
                .tag("status", "SUCCEEDED")
                .timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(250.0);
    }

    @Test
    void ignoresEventsWithoutSchedulerId() {
        listener.onEvent(event(null, STARTED_AT.plusMillis(100)));
        listener.onEvent(event("", STARTED_AT.plusMillis(100)));
        listener.onEvent(event("   ", STARTED_AT.plusMillis(100)));

        assertThat(registry.find("mavencrafted.scheduled.audit.executions").counter()).isNull();
        assertThat(registry.find("mavencrafted.scheduled.audit.duration").timer()).isNull();
    }

    private static ScheduledAuditEvent event(String schedulerId, Instant finishedAt) {
        return ScheduledAuditEvent.builder()
                .executionId(UUID.randomUUID())
                .scheduledMethod("TestScheduler.run")
                .schedulerId(schedulerId)
                .tags(Set.of())
                .status(ScheduledAuditEvent.Status.SUCCEEDED)
                .startedAt(MicrometerScheduledAuditListenerTests.STARTED_AT)
                .finishedAt(finishedAt)
                .build();
    }
}
