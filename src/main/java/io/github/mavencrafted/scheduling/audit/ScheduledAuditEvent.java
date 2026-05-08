package io.github.mavencrafted.scheduling.audit;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
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
    private final String scheduledMethod;
    private final Set<String> tags;
    private final Status status;
    private final Instant startedAt;
    private final Instant finishedAt;
    private final Throwable failure;

    private ScheduledAuditEvent(Builder builder) {
        this(
                builder.executionId,
                builder.scheduledMethod,
                builder.tags,
                builder.status,
                builder.startedAt,
                builder.finishedAt,
                builder.failure
        );
    }

    private ScheduledAuditEvent(
            UUID executionId,
            String scheduledMethod,
            Set<String> tags,
            Status status,
            Instant startedAt,
            Instant finishedAt,
            Throwable failure
    ) {
        this.executionId = Objects.requireNonNull(executionId, "executionId must not be null");
        this.scheduledMethod = Objects.requireNonNull(scheduledMethod, "scheduledMethod must not be null");
        this.tags = Set.copyOf(Objects.requireNonNull(tags, "tags must not be null"));
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt must not be null");
        this.finishedAt = finishedAt;
        this.failure = failure;

        validateState();
    }

    static Builder builder() {
        return new Builder();
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
     * Returns the fully qualified scheduled method name.
     *
     * <p>The value uses the fully qualified method name of the intercepted
     * {@code @Scheduled} method, for example
     * {@code io.github.example.AccountCleanupJob.run}.
     *
     * @return the scheduled method name
     */
    public String getScheduledMethod() {
        return scheduledMethod;
    }

    /**
     * Returns the tags declared for the scheduled task.
     *
     * @return the task tags, possibly empty
     */
    public Set<String> getTags() {
        return this.tags;
    }

    /**
     * Returns whether the scheduled task declares the given tag.
     *
     * @param tag the tag to check
     * @return {@code true} when the tag is present
     */
    public boolean hasTag(String tag) {
        return tag != null && this.tags.contains(tag);
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

    static final class Builder {

        private UUID executionId;
        private String scheduledMethod;
        private Set<String> tags = Set.of();
        private Status status;
        private Instant startedAt;
        private Instant finishedAt;
        private Throwable failure;

        Builder executionId(UUID executionId) {
            this.executionId = executionId;
            return this;
        }

        Builder scheduledMethod(String scheduledMethod) {
            this.scheduledMethod = scheduledMethod;
            return this;
        }

        Builder tags(Set<String> tags) {
            this.tags = tags;
            return this;
        }

        Builder status(Status status) {
            this.status = status;
            return this;
        }

        Builder startedAt(Instant startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        Builder finishedAt(Instant finishedAt) {
            this.finishedAt = finishedAt;
            return this;
        }

        Builder failure(Throwable failure) {
            this.failure = failure;
            return this;
        }

        ScheduledAuditEvent build() {
            return new ScheduledAuditEvent(this);
        }
    }
}
