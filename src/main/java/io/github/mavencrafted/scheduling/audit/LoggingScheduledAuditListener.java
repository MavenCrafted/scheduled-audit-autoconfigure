package io.github.mavencrafted.scheduling.audit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.time.Duration;

public final class LoggingScheduledAuditListener implements ScheduledAuditListener {

    private static final Log logger = LogFactory.getLog(LoggingScheduledAuditListener.class);

    @Override
    public void onEvent(ScheduledAuditEvent event) {
        switch (event.getStatus()) {
            case STARTED -> logStarted(event);
            case SUCCEEDED -> logSucceeded(event);
            case FAILED -> logFailed(event);
        }
    }

    private void logStarted(ScheduledAuditEvent event) {
        if (logger.isDebugEnabled()) {
            logger.debug("Scheduled task started [executionId=" + event.getExecutionId()
                    + ", taskName=" + event.getTaskName()
                    + ", startedAt=" + event.getStartedAt() + "]");
        }
    }

    private void logSucceeded(ScheduledAuditEvent event) {
        if (logger.isDebugEnabled()) {
            logger.debug("Scheduled task succeeded [executionId=" + event.getExecutionId()
                    + ", taskName=" + event.getTaskName()
                    + ", startedAt=" + event.getStartedAt()
                    + ", finishedAt=" + event.getFinishedAt()
                    + ", duration=" + resolveDuration(event) + "]");
        }
    }

    private void logFailed(ScheduledAuditEvent event) {
        logger.error("Scheduled task failed [executionId=" + event.getExecutionId()
                + ", taskName=" + event.getTaskName()
                + ", startedAt=" + event.getStartedAt()
                + ", finishedAt=" + event.getFinishedAt()
                + ", duration=" + resolveDuration(event) + "]", event.getFailure());
    }

    private Duration resolveDuration(ScheduledAuditEvent event) {
        if (event.getFinishedAt() == null) {
            return null;
        }
        return Duration.between(event.getStartedAt(), event.getFinishedAt());
    }
}
