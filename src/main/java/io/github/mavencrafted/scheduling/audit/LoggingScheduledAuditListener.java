package io.github.mavencrafted.scheduling.audit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Default {@link ScheduledAuditListener} that writes scheduled audit events to the application log.
 */
final class LoggingScheduledAuditListener implements ScheduledAuditListener {

    private static final Log logger = LogFactory.getLog(LoggingScheduledAuditListener.class);

    private final boolean includeStacktrace;
    private final Set<String> includeTags;
    private final Set<String> excludeTags;

    LoggingScheduledAuditListener(ScheduledAuditProperties.Logging loggingProperties) {
        this.includeStacktrace = loggingProperties.isIncludeStacktrace();
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

    boolean includesStacktrace() {
        return this.includeStacktrace;
    }

    private void logStarted(ScheduledAuditEvent event) {
        if (logger.isDebugEnabled()) {
            logger.debug("Scheduled task started [executionId=" + event.getExecutionId()
                    + ", scheduledMethod=" + event.getScheduledMethod()
                    + schedulerIdPart(event)
                    + ", startedAt=" + event.getStartedAt() + "]");
        }
    }

    private void logSucceeded(ScheduledAuditEvent event) {
        if (logger.isDebugEnabled()) {
            logger.debug("Scheduled task succeeded [executionId=" + event.getExecutionId()
                    + ", scheduledMethod=" + event.getScheduledMethod()
                    + schedulerIdPart(event)
                    + ", startedAt=" + event.getStartedAt()
                    + ", finishedAt=" + event.getFinishedAt()
                    + ", duration=" + event.getDuration() + "]");
        }
    }

    private void logFailed(ScheduledAuditEvent event) {
        String message = "Scheduled task failed [executionId=" + event.getExecutionId()
                + ", scheduledMethod=" + event.getScheduledMethod()
                + schedulerIdPart(event)
                + ", startedAt=" + event.getStartedAt()
                + ", finishedAt=" + event.getFinishedAt()
                + ", duration=" + event.getDuration()
                + failureSummary(event.getFailure()) + "]";

        if (this.includeStacktrace) {
            logger.error(message, event.getFailure());
            return;
        }

        logger.error(message);
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

    private String failureSummary(Throwable failure) {
        if (failure == null) {
            return "";
        }

        return ", failureType=" + failure.getClass().getName()
                + ", failureMessage=" + failure.getMessage();
    }

    private String schedulerIdPart(ScheduledAuditEvent event) {
        return event.getSchedulerId() != null ? ", schedulerId=" + event.getSchedulerId() : "";
    }
}
