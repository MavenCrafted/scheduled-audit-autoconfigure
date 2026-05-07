package io.github.mavencrafted.scheduling.audit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Default {@link ScheduledAuditListener} that writes scheduled audit events to the application log.
 */
public final class LoggingScheduledAuditListener implements ScheduledAuditListener {

    private static final Log logger = LogFactory.getLog(LoggingScheduledAuditListener.class);

    private final Set<String> includeTags;
    private final Set<String> excludeTags;

    /**
     * Creates a logging listener with default logging settings.
     */
    public LoggingScheduledAuditListener() {
        this(new ScheduledAuditProperties.Logging());
    }

    LoggingScheduledAuditListener(ScheduledAuditProperties.Logging loggingProperties) {
        this.includeTags = normalizeTags(loggingProperties.getIncludeTags());
        this.excludeTags = normalizeTags(loggingProperties.getExcludeTags());
    }

    @Override
    public void onEvent(ScheduledAuditEvent event) {
        if (!shouldLog(event)) {
            return;
        }

        switch (event.getStatus()) {
            case STARTED -> logStarted(event);
            case SUCCEEDED -> logSucceeded(event);
            case FAILED -> logFailed(event);
        }
    }

    boolean shouldLog(ScheduledAuditEvent event) {
        if (matchesAny(event, this.excludeTags)) {
            return false;
        }

        return this.includeTags.isEmpty() || matchesAny(event, this.includeTags);
    }

    private void logStarted(ScheduledAuditEvent event) {
        if (logger.isDebugEnabled()) {
            logger.debug("Scheduled task started [executionId=" + event.getExecutionId()
                    + ", scheduledMethod=" + event.getScheduledMethod()
                    + ", startedAt=" + event.getStartedAt() + "]");
        }
    }

    private void logSucceeded(ScheduledAuditEvent event) {
        if (logger.isDebugEnabled()) {
            logger.debug("Scheduled task succeeded [executionId=" + event.getExecutionId()
                    + ", scheduledMethod=" + event.getScheduledMethod()
                    + ", startedAt=" + event.getStartedAt()
                    + ", finishedAt=" + event.getFinishedAt()
                    + ", duration=" + event.getDuration() + "]");
        }
    }

    private void logFailed(ScheduledAuditEvent event) {
        logger.error("Scheduled task failed [executionId=" + event.getExecutionId()
                + ", scheduledMethod=" + event.getScheduledMethod()
                + ", startedAt=" + event.getStartedAt()
                + ", finishedAt=" + event.getFinishedAt()
                + ", duration=" + event.getDuration() + "]", event.getFailure());
    }

    private boolean matchesAny(ScheduledAuditEvent event, Set<String> tags) {
        for (String tag : tags) {
            if (event.hasTag(tag)) {
                return true;
            }
        }

        return false;
    }

    private Set<String> normalizeTags(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Set.of();
        }

        LinkedHashSet<String> normalizedTags = new LinkedHashSet<>(tags.size());
        for (String tag : tags) {
            if (tag == null) {
                continue;
            }

            String normalizedTag = tag.trim();
            if (!normalizedTag.isEmpty()) {
                normalizedTags.add(normalizedTag);
            }
        }

        return normalizedTags.isEmpty() ? Set.of() : Set.copyOf(normalizedTags);
    }
}
