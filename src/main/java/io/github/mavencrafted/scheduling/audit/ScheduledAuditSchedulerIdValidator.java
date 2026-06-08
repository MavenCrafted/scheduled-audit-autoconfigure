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
        Map<String, String> scheduledMethodsBySchedulerId = new LinkedHashMap<>();

        for (Object bean : this.beanFactory.getBeansOfType(Object.class, false, false).values()) {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            if (targetClass == null) {
                continue;
            }

            ReflectionUtils.doWithMethods(targetClass, method -> validateSchedulerId(method, targetClass, scheduledMethodsBySchedulerId));
        }
    }

    private void validateSchedulerId(Method method, Class<?> targetClass, Map<String, String> scheduledMethodsBySchedulerId) {
        if (!AnnotatedElementUtils.hasAnnotation(method, Scheduled.class)) {
            return;
        }

        ScheduledAudit scheduledAudit = AnnotatedElementUtils.findMergedAnnotation(method, ScheduledAudit.class);
        if (scheduledAudit == null) {
            return;
        }

        String scheduledMethod = ClassUtils.getQualifiedMethodName(method, targetClass);
        String schedulerId = normalizeSchedulerId(scheduledAudit.schedulerId());
        if (schedulerId == null) {
            throw new IllegalStateException("Blank scheduled audit schedulerId found on method '" + scheduledMethod + "'");
        }

        String existingScheduledMethod = scheduledMethodsBySchedulerId.putIfAbsent(schedulerId, scheduledMethod);

        if (existingScheduledMethod != null && !existingScheduledMethod.equals(scheduledMethod)) {
            throw new IllegalStateException("Duplicate scheduled audit schedulerId '" + schedulerId
                    + "' found on methods '" + existingScheduledMethod + "' and '" + scheduledMethod + "'");
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
