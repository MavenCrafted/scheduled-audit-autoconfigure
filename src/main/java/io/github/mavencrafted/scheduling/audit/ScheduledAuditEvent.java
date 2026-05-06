package io.github.mavencrafted.scheduling.audit;

import java.time.Instant;
import java.util.UUID;

public final class ScheduledAuditEvent {

    private final UUID executionId;
    private final String taskName;
    private final Status status;
    private final Instant startedAt;
    private final Instant finishedAt;
    private final Throwable failure;

    private ScheduledAuditEvent(UUID executionId, String taskName, Status status, Instant startedAt, Instant finishedAt, Throwable failure) {
        this.executionId = executionId;
        this.taskName = taskName;
        this.status = status;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.failure = failure;
    }

    public static ScheduledAuditEvent started(UUID executionId, String taskName, Instant startedAt) {
        return new ScheduledAuditEvent(executionId, taskName, Status.STARTED, startedAt, null, null);
    }

    public static ScheduledAuditEvent succeeded(UUID executionId, String taskName, Instant startedAt, Instant finishedAt) {
        return new ScheduledAuditEvent(executionId, taskName, Status.SUCCEEDED, startedAt, finishedAt, null);
    }

    public static ScheduledAuditEvent failed(UUID executionId, String taskName, Instant startedAt, Instant finishedAt, Throwable failure) {
        return new ScheduledAuditEvent(executionId, taskName, Status.FAILED, startedAt, finishedAt, failure);
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public String getTaskName() {
        return taskName;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public Throwable getFailure() {
        return failure;
    }

    public enum Status {
        STARTED,
        SUCCEEDED,
        FAILED
    }
}
