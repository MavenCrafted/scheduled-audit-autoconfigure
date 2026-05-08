package io.github.mavencrafted.scheduling.audit;

import java.time.Duration;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScheduledAuditEventTest {

    @Test
    void createsStartedEvent() {
        UUID executionId = UUID.randomUUID();
        Instant startedAt = Instant.now();

        ScheduledAuditEvent event = ScheduledAuditEvent.builder()
                .executionId(executionId)
                .scheduledMethod("testTask")
                .schedulerId("ACCOUNT_CLEANUP")
                .status(ScheduledAuditEvent.Status.STARTED)
                .startedAt(startedAt)
                .build();

        assertThat(event.getExecutionId()).isEqualTo(executionId);
        assertThat(event.getScheduledMethod()).isEqualTo("testTask");
        assertThat(event.getSchedulerId()).isEqualTo("ACCOUNT_CLEANUP");
        assertThat(event.getTags()).isEmpty();
        assertThat(event.getStatus()).isEqualTo(ScheduledAuditEvent.Status.STARTED);
        assertThat(event.getStartedAt()).isEqualTo(startedAt);
        assertThat(event.getFinishedAt()).isNull();
        assertThat(event.getDuration()).isNull();
        assertThat(event.getFailure()).isNull();
    }

    @Test
    void allowsMissingSchedulerId() {
        ScheduledAuditEvent event = ScheduledAuditEvent.builder()
                .executionId(UUID.randomUUID())
                .scheduledMethod("testTask")
                .status(ScheduledAuditEvent.Status.STARTED)
                .startedAt(Instant.now())
                .build();

        assertThat(event.getSchedulerId()).isNull();
    }

    @Test
    void createsFailedEvent() {
        UUID executionId = UUID.randomUUID();
        Instant startedAt = Instant.now();
        Instant finishedAt = startedAt.plusSeconds(1);
        IllegalStateException failure = new IllegalStateException("boom");

        ScheduledAuditEvent event = ScheduledAuditEvent.builder()
                .executionId(executionId)
                .scheduledMethod("testTask")
                .status(ScheduledAuditEvent.Status.FAILED)
                .startedAt(startedAt)
                .finishedAt(finishedAt)
                .failure(failure)
                .build();

        assertThat(event.getStatus()).isEqualTo(ScheduledAuditEvent.Status.FAILED);
        assertThat(event.getFinishedAt()).isEqualTo(finishedAt);
        assertThat(event.getDuration()).isEqualTo(Duration.ofSeconds(1));
        assertThat(event.getFailure()).isSameAs(failure);
    }

    @Test
    void storesProvidedTags() {
        ScheduledAuditEvent event = ScheduledAuditEvent.builder()
                .executionId(UUID.randomUUID())
                .scheduledMethod("testTask")
                .tags(Set.of("billing", "noisy"))
                .status(ScheduledAuditEvent.Status.STARTED)
                .startedAt(Instant.now())
                .build();

        assertThat(event.getTags()).containsExactlyInAnyOrder("billing", "noisy");
    }

    @Test
    void checksWhetherTagIsPresent() {
        ScheduledAuditEvent event = ScheduledAuditEvent.builder()
                .executionId(UUID.randomUUID())
                .scheduledMethod("testTask")
                .tags(Set.of("billing", "noisy"))
                .status(ScheduledAuditEvent.Status.STARTED)
                .startedAt(Instant.now())
                .build();

        assertThat(event.hasTag("billing")).isTrue();
        assertThat(event.hasTag("ops")).isFalse();
    }

    @Test
    void rejectsNullExecutionId() {
        assertThatThrownBy(() -> ScheduledAuditEvent.builder()
                .executionId(null)
                .scheduledMethod("testTask")
                .status(ScheduledAuditEvent.Status.STARTED)
                .startedAt(Instant.now())
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("executionId must not be null");
    }

    @Test
    void rejectsSucceededEventWithoutFinishedAt() {
        assertThatThrownBy(() -> ScheduledAuditEvent.builder()
                .executionId(UUID.randomUUID())
                .scheduledMethod("testTask")
                .status(ScheduledAuditEvent.Status.SUCCEEDED)
                .startedAt(Instant.now())
                .finishedAt(null)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SUCCEEDED event must define finishedAt");
    }

    @Test
    void rejectsFailedEventWithoutFailure() {
        Instant startedAt = Instant.now();

        assertThatThrownBy(() -> ScheduledAuditEvent.builder()
                .executionId(UUID.randomUUID())
                .scheduledMethod("testTask")
                .status(ScheduledAuditEvent.Status.FAILED)
                .startedAt(startedAt)
                .finishedAt(startedAt.plusSeconds(1))
                .failure(null)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("FAILED event must define failure");
    }
}
