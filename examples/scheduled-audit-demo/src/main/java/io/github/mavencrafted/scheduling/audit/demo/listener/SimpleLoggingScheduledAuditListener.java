package io.github.mavencrafted.scheduling.audit.demo.listener;

import io.github.mavencrafted.scheduling.audit.ScheduledAuditEvent;
import io.github.mavencrafted.scheduling.audit.ScheduledAuditListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class SimpleLoggingScheduledAuditListener implements ScheduledAuditListener {

    private static final Logger logger = LoggerFactory.getLogger(SimpleLoggingScheduledAuditListener.class);

    @Override
    public void onEvent(ScheduledAuditEvent event) {
        switch (event.getStatus()) {
            case STARTED -> logger.info(
                    "Scheduled job started [schedulerId={}, executionId={}, scheduledMethod={}, startedAt={}, tags={}]",
                    event.getSchedulerId(),
                    event.getExecutionId(),
                    event.getScheduledMethod(),
                    event.getStartedAt(),
                    event.getTags()
            );
            case SUCCEEDED -> logger.info(
                    "Scheduled job succeeded [schedulerId={}, executionId={}, duration={}, finishedAt={}, tags={}]",
                    event.getSchedulerId(),
                    event.getExecutionId(),
                    event.getDuration(),
                    event.getFinishedAt(),
                    event.getTags()
            );
            case FAILED -> logger.warn(
                    "Scheduled job failed [schedulerId={}, executionId={}, duration={}, finishedAt={}, errorType={}, error={}, tags={}]",
                    event.getSchedulerId(),
                    event.getExecutionId(),
                    event.getDuration(),
                    event.getFinishedAt(),
                    event.getFailure().getClass().getSimpleName(),
                    event.getFailure().getMessage(),
                    event.getTags()
            );
        }
    }
}
