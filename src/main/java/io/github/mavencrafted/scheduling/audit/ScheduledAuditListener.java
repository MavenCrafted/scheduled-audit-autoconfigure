package io.github.mavencrafted.scheduling.audit;

public interface ScheduledAuditListener {

    void onEvent(ScheduledAuditEvent event);
}
