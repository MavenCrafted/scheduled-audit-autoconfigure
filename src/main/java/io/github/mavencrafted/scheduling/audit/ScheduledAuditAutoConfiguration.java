package io.github.mavencrafted.scheduling.audit;

import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

/**
 * Auto-configuration for scheduled audit support.
 */
@AutoConfiguration
@ConditionalOnClass({ Scheduled.class, Aspect.class })
@ConditionalOnProperty(prefix = "scheduled-audit", name = "enabled", havingValue = "true", matchIfMissing = true)
public final class ScheduledAuditAutoConfiguration {

    /**
     * Registers the scheduled audit aspect.
     *
     * @param listeners the listeners that receive audit events
     * @return the scheduled audit aspect
     */
    @Bean
    @ConditionalOnMissingBean(ScheduledAuditAspect.class)
    public ScheduledAuditAspect scheduledAuditAspect(List<ScheduledAuditListener> listeners) {
        return new ScheduledAuditAspect(listeners);
    }

    /**
     * Registers the default logging listener when logging is enabled and no explicit
     * logging listener bean is present.
     *
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
    public LoggingScheduledAuditListener loggingScheduledAuditListener() {
        return new LoggingScheduledAuditListener();
    }
}
