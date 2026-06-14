package io.github.mavencrafted.scheduling.audit;

import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScheduledAuditAspectTest {

    @Test
    void publishesStartedAndSucceededEvents() throws Throwable {
        List<ScheduledAuditEvent> events = new ArrayList<>();
        ScheduledAuditAspect aspect = new ScheduledAuditAspect(List.of(events::add));
        SampleScheduledBean bean = auditedBean(aspect);

        Object result = bean.run();

        assertThat(result).isEqualTo("done");
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getStatus()).isEqualTo(ScheduledAuditEvent.Status.STARTED);
        assertThat(events.get(1).getStatus()).isEqualTo(ScheduledAuditEvent.Status.SUCCEEDED);
        assertThat(events.get(0).getExecutionId()).isEqualTo(events.get(1).getExecutionId());
        assertThat(events.get(0).getScheduledMethod()).contains("SampleScheduledBean").endsWith(".run");
        assertThat(events.get(0).getSchedulerId()).isEqualTo("ACCOUNT_CLEANUP");
        assertThat(events.get(1).getSchedulerId()).isEqualTo("ACCOUNT_CLEANUP");
        assertThat(events.get(0).getTags()).containsExactlyInAnyOrder("billing", "noisy");
        assertThat(events.get(1).getTags()).containsExactlyInAnyOrder("billing", "noisy");
        assertThat(events.get(1).getFinishedAt()).isNotNull();
        assertThat(events.get(1).getFailure()).isNull();
    }

    @Test
    void publishesFailedEvent() throws Throwable {
        List<ScheduledAuditEvent> events = new ArrayList<>();
        ScheduledAuditAspect aspect = new ScheduledAuditAspect(List.of(events::add));
        SampleScheduledBean bean = auditedBean(aspect);
        IllegalStateException failure = bean.failure();

        assertThatThrownBy(bean::fail)
                .isSameAs(failure);

        assertThat(events).hasSize(2);
        assertThat(events.get(0).getStatus()).isEqualTo(ScheduledAuditEvent.Status.STARTED);
        assertThat(events.get(1).getStatus()).isEqualTo(ScheduledAuditEvent.Status.FAILED);
        assertThat(events.get(0).getExecutionId()).isEqualTo(events.get(1).getExecutionId());
        assertThat(events.get(1).getFailure()).isSameAs(failure);
    }

    @Test
    void publishesEmptyTagsWhenAnnotationIsAbsent() throws Throwable {
        List<ScheduledAuditEvent> events = new ArrayList<>();
        ScheduledAuditAspect aspect = new ScheduledAuditAspect(List.of(events::add));
        SampleScheduledBean bean = auditedBean(aspect);

        Object result = bean.runWithoutAudit();

        assertThat(result).isEqualTo("done");
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getSchedulerId()).isNull();
        assertThat(events.get(1).getSchedulerId()).isNull();
        assertThat(events.get(0).getTags()).isEmpty();
        assertThat(events.get(1).getTags()).isEmpty();
    }

    @Test
    void skipsUnannotatedScheduledMethodsWhenScopeIsAnnotated() throws Throwable {
        List<ScheduledAuditEvent> events = new ArrayList<>();
        ScheduledAuditAspect aspect = new ScheduledAuditAspect(
                List.of(events::add),
                ScheduledAuditProperties.Scope.ANNOTATED
        );
        SampleScheduledBean bean = auditedBean(aspect);

        Object result = bean.runWithoutAudit();

        assertThat(result).isEqualTo("done");
        assertThat(events).isEmpty();
    }

    @Test
    void publishesAnnotatedScheduledMethodsWhenScopeIsAnnotated() throws Throwable {
        List<ScheduledAuditEvent> events = new ArrayList<>();
        ScheduledAuditAspect aspect = new ScheduledAuditAspect(
                List.of(events::add),
                ScheduledAuditProperties.Scope.ANNOTATED
        );
        SampleScheduledBean bean = auditedBean(aspect);

        Object result = bean.run();

        assertThat(result).isEqualTo("done");
        assertThat(events)
                .extracting(ScheduledAuditEvent::getStatus)
                .containsExactly(ScheduledAuditEvent.Status.STARTED, ScheduledAuditEvent.Status.SUCCEEDED);
        assertThat(events.get(0).getSchedulerId()).isEqualTo("ACCOUNT_CLEANUP");
    }

    @Test
    void ignoresListenerFailure() throws Throwable {
        ScheduledAuditAspect aspect = new ScheduledAuditAspect(List.of(event -> {
            throw new IllegalStateException("listener failed");
        }));
        SampleScheduledBean bean = auditedBean(aspect);

        Object result = bean.run();

        assertThat(result).isEqualTo("done");
    }

    @Test
    void publishesEventsToAllListeners() throws Throwable {
        List<ScheduledAuditEvent> firstEvents = new ArrayList<>();
        List<ScheduledAuditEvent> secondEvents = new ArrayList<>();
        List<ScheduledAuditListener> listeners = List.of(firstEvents::add, secondEvents::add);
        ScheduledAuditAspect aspect = new ScheduledAuditAspect(listeners);

        Object result = auditedBean(aspect).run();

        assertThat(result).isEqualTo("done");
        assertThat(firstEvents)
                .extracting(ScheduledAuditEvent::getStatus)
                .containsExactly(ScheduledAuditEvent.Status.STARTED, ScheduledAuditEvent.Status.SUCCEEDED);
        assertThat(secondEvents)
                .extracting(ScheduledAuditEvent::getStatus)
                .containsExactly(ScheduledAuditEvent.Status.STARTED, ScheduledAuditEvent.Status.SUCCEEDED);
        assertThat(firstEvents.get(0).getExecutionId()).isEqualTo(secondEvents.get(0).getExecutionId());
        assertThat(firstEvents.get(1).getExecutionId()).isEqualTo(secondEvents.get(1).getExecutionId());
    }

    @Test
    void listenerFailureDoesNotPreventOtherListeners() throws Throwable {
        List<ScheduledAuditEvent> events = new ArrayList<>();
        ScheduledAuditListener failingListener = event -> {
            throw new IllegalStateException("listener failed");
        };
        ScheduledAuditAspect aspect = new ScheduledAuditAspect(List.of(failingListener, events::add));

        Object result = auditedBean(aspect).run();

        assertThat(result).isEqualTo("done");
        assertThat(events)
                .extracting(ScheduledAuditEvent::getStatus)
                .containsExactly(ScheduledAuditEvent.Status.STARTED, ScheduledAuditEvent.Status.SUCCEEDED);
    }

    private SampleScheduledBean auditedBean(ScheduledAuditAspect aspect) {
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new SampleScheduledBean());
        proxyFactory.addAspect(aspect);
        return proxyFactory.getProxy();
    }

    static class SampleScheduledBean {

        private final IllegalStateException failure = new IllegalStateException("boom");

        @Scheduled(fixedRate = 5000)
        @ScheduledAudit(schedulerId = "ACCOUNT_CLEANUP", tags = {"billing", "noisy"})
        public String run() {
            return "done";
        }

        @Scheduled(fixedRate = 5000)
        public String runWithoutAudit() {
            return "done";
        }

        @Scheduled(fixedRate = 5000)
        public void fail() {
            throw this.failure;
        }

        IllegalStateException failure() {
            return this.failure;
        }
    }
}
