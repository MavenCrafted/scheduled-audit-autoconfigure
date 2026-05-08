package io.github.mavencrafted.scheduling.audit;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingScheduledAuditListenerTest {

    @Test
    void logsAllEventsWhenNoTagFiltersAreConfigured() {
        LoggingScheduledAuditListener listener = new LoggingScheduledAuditListener(new ScheduledAuditProperties.Logging());

        assertThat(listener.shouldLog(eventWithTags("billing", "noisy"))).isTrue();
        assertThat(listener.shouldLog(eventWithTags())).isTrue();
    }

    @Test
    void filtersEventsByIncludedTags() {
        ScheduledAuditProperties.Logging logging = new ScheduledAuditProperties.Logging();
        logging.setIncludeTags(Set.of("billing", "ops"));

        LoggingScheduledAuditListener listener = new LoggingScheduledAuditListener(logging);

        assertThat(listener.shouldLog(eventWithTags("billing"))).isTrue();
        assertThat(listener.shouldLog(eventWithTags("noisy"))).isFalse();
        assertThat(listener.shouldLog(eventWithTags())).isFalse();
    }

    @Test
    void excludedTagsOverrideIncludedTags() {
        ScheduledAuditProperties.Logging logging = new ScheduledAuditProperties.Logging();
        logging.setIncludeTags(Set.of("billing", "ops"));
        logging.setExcludeTags(Set.of("noisy"));

        LoggingScheduledAuditListener listener = new LoggingScheduledAuditListener(logging);

        assertThat(listener.shouldLog(eventWithTags("billing"))).isTrue();
        assertThat(listener.shouldLog(eventWithTags("billing", "noisy"))).isFalse();
    }

    private ScheduledAuditEvent eventWithTags(String... tags) {
        return ScheduledAuditEvent.builder()
                .executionId(UUID.randomUUID())
                .scheduledMethod("testTask")
                .tags(Set.of(tags))
                .status(ScheduledAuditEvent.Status.STARTED)
                .startedAt(Instant.now())
                .build();
    }
}
