package io.github.mavencrafted.scheduling.audit;

import java.time.Duration;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScheduledAuditEventTest {

    @Test
    void createsStartedEvent() {
        UUID executionId = UUID.randomUUID();
        Instant startedAt = Instant.now();

        ScheduledAuditEvent event = ScheduledAuditEvent.started(executionId, "testTask", startedAt);

        assertThat(event.getExecutionId()).isEqualTo(executionId);
        assertThat(event.getTaskName()).isEqualTo("testTask");
        assertThat(event.getStatus()).isEqualTo(ScheduledAuditEvent.Status.STARTED);
        assertThat(event.getStartedAt()).isEqualTo(startedAt);
        assertThat(event.getFinishedAt()).isNull();
        assertThat(event.getDuration()).isNull();
        assertThat(event.getFailure()).isNull();
    }

    @Test
    void createsFailedEvent() {
        UUID executionId = UUID.randomUUID();
        Instant startedAt = Instant.now();
        Instant finishedAt = startedAt.plusSeconds(1);
        IllegalStateException failure = new IllegalStateException("boom");

        ScheduledAuditEvent event = ScheduledAuditEvent.failed(
                executionId,
                "testTask",
                startedAt,
                finishedAt,
                failure
        );

        assertThat(event.getStatus()).isEqualTo(ScheduledAuditEvent.Status.FAILED);
        assertThat(event.getFinishedAt()).isEqualTo(finishedAt);
        assertThat(event.getDuration()).isEqualTo(Duration.ofSeconds(1));
        assertThat(event.getFailure()).isSameAs(failure);
    }

    @Test
    void rejectsNullExecutionId() {
        assertThatThrownBy(() -> ScheduledAuditEvent.started(null, "testTask", Instant.now()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("executionId must not be null");
    }

    @Test
    void rejectsSucceededEventWithoutFinishedAt() {
        assertThatThrownBy(() -> ScheduledAuditEvent.succeeded(
                UUID.randomUUID(),
                "testTask",
                Instant.now(),
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SUCCEEDED event must define finishedAt");
    }

    @Test
    void rejectsFailedEventWithoutFailure() {
        Instant startedAt = Instant.now();

        assertThatThrownBy(() -> ScheduledAuditEvent.failed(
                UUID.randomUUID(),
                "testTask",
                startedAt,
                startedAt.plusSeconds(1),
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("FAILED event must define failure");
    }
}
