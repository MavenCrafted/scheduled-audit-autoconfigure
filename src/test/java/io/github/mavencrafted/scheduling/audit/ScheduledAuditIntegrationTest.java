package io.github.mavencrafted.scheduling.audit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(
        classes = ScheduledAuditIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class ScheduledAuditIntegrationTest {

    @Autowired
    private TestScheduledAuditListener listener;

    @Test
    void interceptsScheduledMethodExecution() {
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(listener.events()).hasSize(2));

        List<ScheduledAuditEvent> events = listener.events();
        ScheduledAuditEvent started = events.get(0);
        ScheduledAuditEvent succeeded = events.get(1);

        assertThat(events)
                .extracting(ScheduledAuditEvent::getStatus)
                .containsExactly(ScheduledAuditEvent.Status.STARTED, ScheduledAuditEvent.Status.SUCCEEDED);
        assertThat(started.getExecutionId()).isEqualTo(succeeded.getExecutionId());
        assertThat(started.getTaskName())
                .isEqualTo("io.github.mavencrafted.scheduling.audit.ScheduledAuditIntegrationTest$SampleScheduledBean.run");
        assertThat(started.getFailure()).isNull();
        assertThat(succeeded.getFailure()).isNull();
        assertThat(succeeded.getFinishedAt()).isNotNull();
        assertThat(succeeded.getFinishedAt()).isAfterOrEqualTo(started.getStartedAt());
    }

    @SpringBootApplication
    @EnableScheduling
    static class TestApplication {

        @Bean
        TestScheduledAuditListener testScheduledAuditListener() {
            return new TestScheduledAuditListener();
        }

        @Bean
        SampleScheduledBean sampleScheduledBean() {
            return new SampleScheduledBean();
        }
    }

    static class TestScheduledAuditListener implements ScheduledAuditListener {

        private final CopyOnWriteArrayList<ScheduledAuditEvent> events = new CopyOnWriteArrayList<>();

        @Override
        public void onEvent(ScheduledAuditEvent event) {
            this.events.add(event);
        }

        List<ScheduledAuditEvent> events() {
            return this.events;
        }
    }

    public static class SampleScheduledBean {

        @Scheduled(initialDelay = 0, fixedDelay = 600000)
        public void run() {
        }
    }
}
