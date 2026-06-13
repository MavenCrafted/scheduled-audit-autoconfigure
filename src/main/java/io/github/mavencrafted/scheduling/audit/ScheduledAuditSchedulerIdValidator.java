package io.github.mavencrafted.scheduling.audit;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Validates scheduler identifier declarations on Spring {@code @Scheduled} methods at startup.
 *
 * <p>The validator inspects bean type metadata instead of resolving bean instances, so lazy
 * scheduled beans can be validated without being instantiated. Non-scheduled methods are ignored.
 *
 * <p>When {@link ScheduledAuditProperties.SchedulerIdPolicy#REQUIRED} is configured, every
 * scheduled method must declare {@link ScheduledAudit}. For methods that do declare
 * {@code @ScheduledAudit}, the configured {@code schedulerId} is trimmed, must not be blank,
 * and must be unique across all scheduled bean instances in the application.
 *
 * <p>Validation failures include a scheduled job reference in the form
 * {@code beanName:fully.qualified.MethodName}.
 */
final class ScheduledAuditSchedulerIdValidator implements SmartInitializingSingleton {

    private final ListableBeanFactory beanFactory;
    private final ScheduledAuditProperties.SchedulerIdPolicy schedulerIdPolicy;

    ScheduledAuditSchedulerIdValidator(ListableBeanFactory beanFactory, ScheduledAuditProperties.SchedulerIdPolicy schedulerIdPolicy) {
        this.beanFactory = beanFactory;
        this.schedulerIdPolicy = schedulerIdPolicy != null ? schedulerIdPolicy : ScheduledAuditProperties.SchedulerIdPolicy.OPTIONAL;
    }

    @Override
    public void afterSingletonsInstantiated() {
        Map<String, String> scheduledJobsBySchedulerId = new LinkedHashMap<>();

        for (String beanName : this.beanFactory.getBeanNamesForType(Object.class, false, false)) {
            Class<?> type = this.beanFactory.getType(beanName, false);
            if (type == null) {
                continue;
            }

            ReflectionUtils.doWithMethods(type,
                    method -> validateSchedulerId(beanName, method, type, scheduledJobsBySchedulerId));
        }
    }

    private void validateSchedulerId(
            String beanName,
            Method method,
            Class<?> targetClass,
            Map<String, String> scheduledJobsBySchedulerId
    ) {
        if (!AnnotatedElementUtils.hasAnnotation(method, Scheduled.class)) {
            return;
        }

        ScheduledAudit scheduledAudit = AnnotatedElementUtils.findMergedAnnotation(method, ScheduledAudit.class);
        if (scheduledAudit == null) {
            if (this.schedulerIdPolicy == ScheduledAuditProperties.SchedulerIdPolicy.REQUIRED) {
                throw new IllegalStateException("Missing scheduled audit schedulerId on scheduled job '"
                        + scheduledJob(beanName, method, targetClass)
                        + "' because scheduled-audit.scheduler-id-policy=required");
            }

            return;
        }

        String scheduledJob = scheduledJob(beanName, method, targetClass);
        String schedulerId = normalizeSchedulerId(scheduledAudit.schedulerId());
        if (schedulerId == null) {
            throw new IllegalStateException("Blank scheduled audit schedulerId found on scheduled job '" + scheduledJob + "'");
        }

        String existingScheduledJob = scheduledJobsBySchedulerId.putIfAbsent(schedulerId, scheduledJob);

        if (existingScheduledJob != null && !existingScheduledJob.equals(scheduledJob)) {
            throw new IllegalStateException("Duplicate scheduled audit schedulerId '" + schedulerId
                    + "' found on scheduled jobs '" + existingScheduledJob + "' and '" + scheduledJob + "'");
        }
    }

    private String scheduledJob(String beanName, Method method, Class<?> targetClass) {
        String scheduledMethod = ClassUtils.getQualifiedMethodName(method, targetClass);
        return beanName + ":" + scheduledMethod;
    }

    private String normalizeSchedulerId(String schedulerId) {
        if (schedulerId == null) {
            return null;
        }

        String normalizedSchedulerId = schedulerId.trim();
        return normalizedSchedulerId.isEmpty() ? null : normalizedSchedulerId;
    }
}
