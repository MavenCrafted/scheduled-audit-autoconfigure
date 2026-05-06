package io.github.mavencrafted.scheduling.audit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScheduledAuditAspectTest {

    @Test
    void publishesStartedAndSucceededEvents() throws Throwable {
        List<ScheduledAuditEvent> events = new ArrayList<>();
        ScheduledAuditAspect aspect = new ScheduledAuditAspect(List.of(events::add));
        Method method = scheduledMethod();

        Object result = aspect.audit(joinPoint(method, "done"), scheduled(method));

        assertThat(result).isEqualTo("done");
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getStatus()).isEqualTo(ScheduledAuditEvent.Status.STARTED);
        assertThat(events.get(1).getStatus()).isEqualTo(ScheduledAuditEvent.Status.SUCCEEDED);
        assertThat(events.get(0).getExecutionId()).isEqualTo(events.get(1).getExecutionId());
        assertThat(events.get(0).getTaskName()).contains("SampleScheduledBean").endsWith(".run");
        assertThat(events.get(1).getFinishedAt()).isNotNull();
        assertThat(events.get(1).getFailure()).isNull();
    }

    @Test
    void publishesFailedEvent() throws Throwable {
        List<ScheduledAuditEvent> events = new ArrayList<>();
        ScheduledAuditAspect aspect = new ScheduledAuditAspect(List.of(events::add));
        Method method = scheduledMethod();
        IllegalStateException failure = new IllegalStateException("boom");

        assertThatThrownBy(() -> aspect.audit(joinPoint(method, failure), scheduled(method)))
                .isSameAs(failure);

        assertThat(events).hasSize(2);
        assertThat(events.get(0).getStatus()).isEqualTo(ScheduledAuditEvent.Status.STARTED);
        assertThat(events.get(1).getStatus()).isEqualTo(ScheduledAuditEvent.Status.FAILED);
        assertThat(events.get(0).getExecutionId()).isEqualTo(events.get(1).getExecutionId());
        assertThat(events.get(1).getFailure()).isSameAs(failure);
    }

    @Test
    void ignoresListenerFailure() throws Throwable {
        ScheduledAuditAspect aspect = new ScheduledAuditAspect(List.of(event -> {
            throw new IllegalStateException("listener failed");
        }));
        Method method = scheduledMethod();

        Object result = aspect.audit(joinPoint(method, "done"), scheduled(method));

        assertThat(result).isEqualTo("done");
    }

    @Test
    void publishesEventsToAllListeners() throws Throwable {
        List<ScheduledAuditEvent> firstEvents = new ArrayList<>();
        List<ScheduledAuditEvent> secondEvents = new ArrayList<>();
        List<ScheduledAuditListener> listeners = List.of(firstEvents::add, secondEvents::add);
        ScheduledAuditAspect aspect = new ScheduledAuditAspect(listeners);
        Method method = scheduledMethod();

        Object result = aspect.audit(joinPoint(method, "done"), scheduled(method));

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
        Method method = scheduledMethod();

        Object result = aspect.audit(joinPoint(method, "done"), scheduled(method));

        assertThat(result).isEqualTo("done");
        assertThat(events)
                .extracting(ScheduledAuditEvent::getStatus)
                .containsExactly(ScheduledAuditEvent.Status.STARTED, ScheduledAuditEvent.Status.SUCCEEDED);
    }

    private ProceedingJoinPoint joinPoint(Method method, Object result) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(new SampleScheduledBean());
        when(signature.getMethod()).thenReturn(method);

        if (result instanceof Throwable throwable) {
            when(joinPoint.proceed()).thenThrow(throwable);
        }
        else {
            when(joinPoint.proceed()).thenReturn(result);
        }

        return joinPoint;
    }

    private Method scheduledMethod() throws NoSuchMethodException {
        return SampleScheduledBean.class.getMethod("run");
    }

    private Scheduled scheduled(Method method) {
        return method.getAnnotation(Scheduled.class);
    }

    static final class SampleScheduledBean {

        @Scheduled(fixedRate = 5000)
        public String run() {
            return "done";
        }
    }
}
