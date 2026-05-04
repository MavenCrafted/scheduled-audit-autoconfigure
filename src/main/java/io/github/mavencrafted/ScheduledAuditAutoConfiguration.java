package io.github.mavencrafted;

import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;

@AutoConfiguration
@ConditionalOnClass({ Scheduled.class, Aspect.class })
@ConditionalOnProperty(prefix = "scheduled-audit", name = "enabled", havingValue = "true", matchIfMissing = true)
public final class ScheduledAuditAutoConfiguration {
}
