package io.github.mavencrafted.scheduling.audit.demo.scheduler;

import io.github.mavencrafted.scheduling.audit.ScheduledAudit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class ReportGenerationScheduler {

    private static final Log logger = LogFactory.getLog(ReportGenerationScheduler.class);

    private static final long INITIAL_DELAY_MILLIS = 2_000;
    private static final long INTERVAL_MILLIS = 10_000;

    @Scheduled(initialDelay = INITIAL_DELAY_MILLIS, fixedRate = INTERVAL_MILLIS)
    @ScheduledAudit(
            schedulerId = "daily-report-generation",
            tags = {"reporting", "demo"}
    )
    void generateReports() {
        logger.info("Successful job executed");
    }
}
