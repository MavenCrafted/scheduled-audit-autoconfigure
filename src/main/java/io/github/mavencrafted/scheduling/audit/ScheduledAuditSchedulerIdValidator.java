package io.github.mavencrafted.scheduling.audit;

import org.springframework.aop.support.AopUtils;
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
 * Validates that declared scheduled audit scheduler identifiers are unique.
 */
final class ScheduledAuditSchedulerIdValidator implements SmartInitializingSingleton {

    private final ListableBeanFactory beanFactory;

    ScheduledAuditSchedulerIdValidator(ListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public void afterSingletonsInstantiated() {
        Map<String, String> scheduledJobsBySchedulerId = new LinkedHashMap<>();

        for (String beanName : this.beanFactory.getBeanNamesForType(Object.class, false, false)) {
            Object bean = this.beanFactory.getBean(beanName);
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            if (targetClass == null) {
                continue;
            }

            ReflectionUtils.doWithMethods(targetClass,
                    method -> validateSchedulerId(beanName, method, targetClass, scheduledJobsBySchedulerId));
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
            return;
        }

        String scheduledMethod = ClassUtils.getQualifiedMethodName(method, targetClass);
        String scheduledJob = beanName + ":" + scheduledMethod;
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

    private String normalizeSchedulerId(String schedulerId) {
        if (schedulerId == null) {
            return null;
        }

        String normalizedSchedulerId = schedulerId.trim();
        return normalizedSchedulerId.isEmpty() ? null : normalizedSchedulerId;
    }
}
