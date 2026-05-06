package io.github.mavencrafted.scheduling.audit;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a lifecycle event emitted for a single scheduled job execution.
 *
 * <p>The {@code executionId} is unique per scheduled job run and is shared by the
 * {@link Status#STARTED} and matching terminal {@link Status#SUCCEEDED} or
 * {@link Status#FAILED} event for that same execution.
 */
public final class ScheduledAuditEvent {

    private final UUID executionId;
    private final String taskName;
    private final Status status;
    private final Instant startedAt;
    private final Instant finishedAt;
    private final Throwable failure;

    private ScheduledAuditEvent(UUID executionId, String taskName, Status status, Instant startedAt, Instant finishedAt, Throwable failure) {
        this.executionId = Objects.requireNonNull(executionId, "executionId must not be null");
        this.taskName = Objects.requireNonNull(taskName, "taskName must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt must not be null");
        this.finishedAt = finishedAt;
        this.failure = failure;

        validateState();
    }

    /**
     * Creates a started event.
     *
     * @param executionId the execution identifier
     * @param taskName the scheduled task name
     * @param startedAt the execution start time
     * @return the created event
     */
    public static ScheduledAuditEvent started(UUID executionId, String taskName, Instant startedAt) {
        return new ScheduledAuditEvent(executionId, taskName, Status.STARTED, startedAt, null, null);
    }

    /**
     * Creates a succeeded event.
     *
     * @param executionId the execution identifier
     * @param taskName the scheduled task name
     * @param startedAt the execution start time
     * @param finishedAt the execution completion time
     * @return the created event
     */
    public static ScheduledAuditEvent succeeded(UUID executionId, String taskName, Instant startedAt, Instant finishedAt) {
        return new ScheduledAuditEvent(executionId, taskName, Status.SUCCEEDED, startedAt, finishedAt, null);
    }

    /**
     * Creates a failed event.
     *
     * @param executionId the execution identifier
     * @param taskName the scheduled task name
     * @param startedAt the execution start time
     * @param finishedAt the execution completion time
     * @param failure the failure raised by the scheduled job
     * @return the created event
     */
    public static ScheduledAuditEvent failed(UUID executionId, String taskName, Instant startedAt, Instant finishedAt, Throwable failure) {
        return new ScheduledAuditEvent(executionId, taskName, Status.FAILED, startedAt, finishedAt, failure);
    }

    /**
     * Returns the execution identifier for the scheduled job run.
     *
     * <p>This identifier is unique per scheduled execution and remains the same across
     * the started and terminal event emitted for that execution.
     *
     * @return the execution identifier
     */
    public UUID getExecutionId() {
        return executionId;
    }
    
    /**
     * Returns the scheduled task name.
     *
     * <p>The value uses the fully qualified method name of the intercepted
     * {@code @Scheduled} method, for example
     * {@code io.github.example.AccountCleanupJob.run}.
     *
     * @return the task name
     */
    public String getTaskName() {
        return taskName;
    }

    /**
     * Returns the execution status.
     *
     * @return the execution status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Returns the execution start time.
     *
     * @return the execution start time
     */
    public Instant getStartedAt() {
        return startedAt;
    }

    /**
     * Returns the execution completion time.
     *
     * @return the execution completion time, or {@code null} when the execution has not completed
     */
    public Instant getFinishedAt() {
        return finishedAt;
    }

    /**
     * Returns the execution duration.
     *
     * @return the execution duration, or {@code null} when the execution has not completed
     */
    public Duration getDuration() {
        if (this.finishedAt == null) {
            return null;
        }
        return Duration.between(this.startedAt, this.finishedAt);
    }

    /**
     * Returns the failure raised by the scheduled job.
     *
     * @return the failure, or {@code null} when the execution completed successfully or has not completed
     */
    public Throwable getFailure() {
        return failure;
    }

    private void validateState() {
        if (this.finishedAt != null && this.finishedAt.isBefore(this.startedAt)) {
            throw new IllegalArgumentException("finishedAt must not be before startedAt");
        }

        switch (this.status) {
            case STARTED -> {
                if (this.finishedAt != null) {
                    throw new IllegalArgumentException("STARTED event must not define finishedAt");
                }
                if (this.failure != null) {
                    throw new IllegalArgumentException("STARTED event must not define failure");
                }
            }
            case SUCCEEDED -> {
                if (this.finishedAt == null) {
                    throw new IllegalArgumentException("SUCCEEDED event must define finishedAt");
                }
                if (this.failure != null) {
                    throw new IllegalArgumentException("SUCCEEDED event must not define failure");
                }
            }
            case FAILED -> {
                if (this.finishedAt == null) {
                    throw new IllegalArgumentException("FAILED event must define finishedAt");
                }
                if (this.failure == null) {
                    throw new IllegalArgumentException("FAILED event must define failure");
                }
            }
        }
    }

    /**
     * Defines the lifecycle state of a scheduled job execution.
     */
    public enum Status {
        /**
         * The scheduled job has started.
         */
        STARTED,
        /**
         * The scheduled job completed successfully.
         */
        SUCCEEDED,
        /**
         * The scheduled job completed with a failure.
         */
        FAILED
    }
}
