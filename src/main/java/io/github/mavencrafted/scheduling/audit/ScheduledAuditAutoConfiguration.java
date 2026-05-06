package io.github.mavencrafted.scheduling.audit;

import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

@AutoConfiguration
@ConditionalOnClass({ Scheduled.class, Aspect.class })
@ConditionalOnProperty(prefix = "scheduled-audit", name = "enabled", havingValue = "true", matchIfMissing = true)
public final class ScheduledAuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ScheduledAuditAspect.class)
    public ScheduledAuditAspect scheduledAuditAspect(List<ScheduledAuditListener> listeners) {
        return new ScheduledAuditAspect(listeners);
    }

    @Bean
    @ConditionalOnMissingBean(ScheduledAuditListener.class)
    public LoggingScheduledAuditListener loggingScheduledAuditListener() {
        return new LoggingScheduledAuditListener();
    }
}
