package io.github.mavencrafted.scheduling.audit;

import io.micrometer.core.instrument.MeterRegistry;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

/**
 * Auto-configuration for scheduled audit support.
 */
@AutoConfiguration
@EnableConfigurationProperties(ScheduledAuditProperties.class)
@ConditionalOnClass({ Scheduled.class, Aspect.class })
@ConditionalOnProperty(prefix = "scheduled-audit", name = "enabled", havingValue = "true", matchIfMissing = true)
public final class ScheduledAuditAutoConfiguration {

    /**
     * Registers the scheduled audit aspect.
     *
     * @param listeners the listeners that receive audit events
     * @param scheduledAuditProperties the scheduled audit configuration properties
     * @return the scheduled audit aspect
     */
    @Bean
    @ConditionalOnMissingBean(ScheduledAuditAspect.class)
    ScheduledAuditAspect scheduledAuditAspect(List<ScheduledAuditListener> listeners, ScheduledAuditProperties scheduledAuditProperties) {
        return new ScheduledAuditAspect(listeners, scheduledAuditProperties.getScope());
    }

    /**
     * Registers the startup validator for unique scheduled audit scheduler identifiers.
     *
     * @param beanFactory the bean factory used to inspect scheduled beans
     * @return the scheduler identifier validator
     */
    @Bean
    @ConditionalOnMissingBean(ScheduledAuditSchedulerIdValidator.class)
    ScheduledAuditSchedulerIdValidator scheduledAuditSchedulerIdValidator(ListableBeanFactory beanFactory) {
        return new ScheduledAuditSchedulerIdValidator(beanFactory);
    }

    /**
     * Registers the default logging listener when logging is enabled and no explicit
     * logging listener bean is present.
     *
     * @param scheduledAuditProperties the scheduled audit configuration properties
     * @return the default logging listener
     */
    @Bean
    @ConditionalOnMissingBean(LoggingScheduledAuditListener.class)
    @ConditionalOnProperty(
            prefix = "scheduled-audit.logging",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    LoggingScheduledAuditListener loggingScheduledAuditListener(ScheduledAuditProperties scheduledAuditProperties) {
        return new LoggingScheduledAuditListener(scheduledAuditProperties.getLogging());
    }

    /**
    * Configuration for publishing scheduled audit metrics when Micrometer is available.
    */
    @ConditionalOnClass(MeterRegistry.class)
    static class MicrometerConfiguration {

        /**
         * Registers the Micrometer metrics listener when Micrometer is available,
         * metrics are enabled, and no explicit metrics listener bean is present.
         *
         * @param meterRegistry the Micrometer meter registry used to record scheduled audit metrics
         * @return the Micrometer scheduled audit listener
         */
        @Bean
        @ConditionalOnMissingBean(MicrometerScheduledAuditListener.class)
        @ConditionalOnProperty(
                prefix = "scheduled-audit.metrics",
                name = "enabled",
                havingValue = "true"
        )
        MicrometerScheduledAuditListener micrometerScheduledAuditListener(MeterRegistry meterRegistry) {
            return new MicrometerScheduledAuditListener(meterRegistry);
        }
    }
}
