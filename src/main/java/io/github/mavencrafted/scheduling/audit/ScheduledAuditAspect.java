package io.github.mavencrafted.scheduling.audit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aspect that emits {@link ScheduledAuditEvent} instances around {@code @Scheduled} method execution.
 */
@Aspect
final class ScheduledAuditAspect {

    private static final Log logger = LogFactory.getLog(ScheduledAuditAspect.class);

    private final ScheduledAuditProperties.Scope scope;
    private final List<ScheduledAuditListener> scheduledAuditListeners;
    private final Map<Method, ScheduledAuditDescriptor> scheduledAuditDescriptors = new ConcurrentHashMap<>();

    /**
     * Creates a new aspect backed by the provided listeners.
     *
     * @param scheduledAuditListeners the listeners that receive audit events
     */
    ScheduledAuditAspect(List<ScheduledAuditListener> scheduledAuditListeners) {
        this(scheduledAuditListeners, ScheduledAuditProperties.Scope.ALL);
    }

    /**
     * Creates a new aspect backed by the provided listeners and audit scope.
     *
     * @param scheduledAuditListeners the listeners that receive audit events
     * @param scope which scheduled methods should emit audit events
     */
    ScheduledAuditAspect(List<ScheduledAuditListener> scheduledAuditListeners, ScheduledAuditProperties.Scope scope) {
        this.scope = scope != null ? scope : ScheduledAuditProperties.Scope.ALL;
        this.scheduledAuditListeners = List.copyOf(scheduledAuditListeners);
    }

    /**
     * Wraps a scheduled method invocation and publishes audit events for its lifecycle.
     *
     * @param joinPoint the intercepted scheduled method invocation
     * @param scheduled the scheduled annotation that matched the invocation
     * @return the scheduled method result
     * @throws Throwable when the scheduled method fails
     */
    @Around("@annotation(scheduled)")
    public Object audit(ProceedingJoinPoint joinPoint, Scheduled scheduled) throws Throwable {
        Method method = resolveScheduledMethod(joinPoint);
        ScheduledAuditDescriptor descriptor = resolveDescriptor(method);
        if (!shouldAudit(descriptor)) {
            return joinPoint.proceed();
        }

        UUID executionId = UUID.randomUUID();
        Instant startedAt = Instant.now();
        invokeListenersSafely(baseEventBuilder(executionId, descriptor, startedAt)
                .status(ScheduledAuditEvent.Status.STARTED)
                .build());
        try {
            Object result = joinPoint.proceed();
            invokeListenersSafely(baseEventBuilder(executionId, descriptor, startedAt)
                    .status(ScheduledAuditEvent.Status.SUCCEEDED)
                    .finishedAt(Instant.now())
                    .build());
            return result;
        }
        catch (Throwable throwable) {
            invokeListenersSafely(baseEventBuilder(executionId, descriptor, startedAt)
                    .status(ScheduledAuditEvent.Status.FAILED)
                    .finishedAt(Instant.now())
                    .failure(throwable)
                    .build());
            throw throwable;
        }
    }

    private ScheduledAuditEvent.Builder baseEventBuilder(
            UUID executionId,
            ScheduledAuditDescriptor descriptor,
            Instant startedAt
    ) {
        return ScheduledAuditEvent.builder()
                .executionId(executionId)
                .scheduledMethod(descriptor.scheduledMethod())
                .schedulerId(descriptor.schedulerId())
                .tags(descriptor.tags())
                .startedAt(startedAt);
    }

    private void invokeListenersSafely(ScheduledAuditEvent event) {
        for (ScheduledAuditListener listener : scheduledAuditListeners) {
            try {
                listener.onEvent(event);
            }
            catch (RuntimeException ex) {
                logger.warn("Scheduled audit listener failed: " + listener.getClass().getName(), ex);
            }
        }
    }

    private Method resolveScheduledMethod(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Object target = joinPoint.getTarget();
        Class<?> targetClass = (target != null ? AopUtils.getTargetClass(target) : method.getDeclaringClass());
        return AopUtils.getMostSpecificMethod(method, targetClass);
    }

    private ScheduledAuditDescriptor resolveDescriptor(Method method) {
        return this.scheduledAuditDescriptors.computeIfAbsent(method, this::extractDescriptor);
    }

    private ScheduledAuditDescriptor extractDescriptor(Method method) {
        String scheduledMethod = ClassUtils.getQualifiedMethodName(method, method.getDeclaringClass());
        try {
            ScheduledAudit scheduledAudit = AnnotatedElementUtils.findMergedAnnotation(method, ScheduledAudit.class);
            if (scheduledAudit == null) {
                return new ScheduledAuditDescriptor(scheduledMethod, null, Set.of(), false);
            }

            return new ScheduledAuditDescriptor(
                    scheduledMethod,
                    resolveSchedulerId(scheduledAudit),
                    resolveTags(scheduledAudit),
                    true
            );
        }
        catch (RuntimeException ex) {
            logger.warn("Failed to resolve ScheduledAudit metadata for scheduled method: " + scheduledMethod, ex);
            return new ScheduledAuditDescriptor(scheduledMethod, null, Set.of(), true);
        }
    }

    private boolean shouldAudit(ScheduledAuditDescriptor descriptor) {
        return this.scope == ScheduledAuditProperties.Scope.ALL || descriptor.hasScheduledAudit();
    }

    private String resolveSchedulerId(ScheduledAudit scheduledAudit) {
        String rawSchedulerId = scheduledAudit.schedulerId();
        if (rawSchedulerId == null) {
            return null;
        }

        String normalizedSchedulerId = rawSchedulerId.trim();
        return normalizedSchedulerId.isEmpty() ? null : normalizedSchedulerId;
    }

    private Set<String> resolveTags(ScheduledAudit scheduledAudit) {
        String[] rawTags = scheduledAudit.tags();
        if (rawTags.length == 0) {
            return Set.of();
        }

        LinkedHashSet<String> normalizedTags = new LinkedHashSet<>(rawTags.length);
        for (String rawTag : rawTags) {
            if (rawTag == null) {
                continue;
            }

            String normalizedTag = rawTag.trim();
            if (!normalizedTag.isEmpty()) {
                normalizedTags.add(normalizedTag);
            }
        }

        return normalizedTags.isEmpty() ? Set.of() : Set.copyOf(normalizedTags);
    }

    private record ScheduledAuditDescriptor(
            String scheduledMethod,
            String schedulerId,
            Set<String> tags,
            boolean hasScheduledAudit
    ) {
    }
}
