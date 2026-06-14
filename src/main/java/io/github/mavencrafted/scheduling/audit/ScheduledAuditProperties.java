package io.github.mavencrafted.scheduling.audit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Configuration properties for scheduled audit support.
 */
@ConfigurationProperties(prefix = "scheduled-audit")
public class ScheduledAuditProperties {

    /**
     * Whether scheduled audit support is enabled.
     */
    private boolean enabled = true;

    /**
     * Which scheduled methods should emit audit events.
     */
    private Scope scope = Scope.ALL;

    /**
     * Whether scheduler identifiers are optional or required for scheduled methods.
     */
    private SchedulerIdPolicy schedulerIdPolicy = SchedulerIdPolicy.OPTIONAL;

    /**
     * Logging configuration for the default scheduled audit listener.
     */
    private final Logging logging = new Logging();

    /**
     * Metrics configuration for the Micrometer scheduled audit listener.
     */
    private final Metrics metrics = new Metrics();

    /**
     * Creates scheduled audit properties with default values.
     */
    public ScheduledAuditProperties() {
    }

    /**
     * Returns whether scheduled audit support is enabled.
     *
     * @return {@code true} when scheduled audit support is enabled
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Sets whether scheduled audit support is enabled.
     *
     * @param enabled whether scheduled audit support is enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns which scheduled methods should emit audit events.
     *
     * @return the scheduled audit scope
     */
    public Scope getScope() {
        return this.scope;
    }

    /**
     * Sets which scheduled methods should emit audit events.
     *
     * @param scope the scheduled audit scope
     */
    public void setScope(Scope scope) {
        this.scope = (scope != null ? scope : Scope.ALL);
    }

    /**
     * Returns whether scheduler identifiers are optional or required for scheduled methods.
     *
     * @return the configured scheduler identifier policy
     */
    public SchedulerIdPolicy getSchedulerIdPolicy() {
        return this.schedulerIdPolicy;
    }

    /**
     * Sets the scheduler identifier validation policy.
     *
     * <p>When set to {@code null}, the policy falls back to {@link SchedulerIdPolicy#OPTIONAL}.
     *
     * @param schedulerIdPolicy the scheduler identifier validation policy
     */
    public void setSchedulerIdPolicy(SchedulerIdPolicy schedulerIdPolicy) {
        this.schedulerIdPolicy = (schedulerIdPolicy != null ? schedulerIdPolicy : SchedulerIdPolicy.OPTIONAL);
    }

    /**
     * Returns the logging properties for the default scheduled audit listener.
     *
     * @return the logging properties
     */
    public Logging getLogging() {
        return this.logging;
    }

    /**
     * Returns the metrics properties for the Micrometer scheduled audit listener.
     *
     * @return the metrics properties
     */
    public Metrics getMetrics() {
        return this.metrics;
    }

    /**
     * Logging properties for the default scheduled audit listener.
     */
    public static class Logging {

        /**
         * Whether the default logging listener is enabled.
         */
        private boolean enabled = true;

        /**
         * Whether failed-event logs should include the full failure stack trace.
         */
        private boolean includeStacktrace = false;

        /**
         * Tags that must be present for the default logging listener to log an event.
         */
        private Set<String> includeTags = new LinkedHashSet<>();

        /**
         * Tags that suppress logging when present on an event.
         */
        private Set<String> excludeTags = new LinkedHashSet<>();

        /**
         * Creates logging properties with default values.
         */
        public Logging() {
        }

        /**
         * Returns whether the default logging listener is enabled.
         *
         * @return {@code true} when the default logging listener is enabled
         */
        public boolean isEnabled() {
            return this.enabled;
        }

        /**
         * Sets whether the default logging listener is enabled.
         *
         * @param enabled whether the default logging listener is enabled
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Returns whether failed-event logs include the full stack trace.
         *
         * @return {@code true} when failed-event logs include the full stack trace
         */
        public boolean isIncludeStacktrace() {
            return this.includeStacktrace;
        }

        /**
         * Sets whether failed-event logs include the full stack trace.
         *
         * @param includeStacktrace whether failed-event logs include the full stack trace
         */
        public void setIncludeStacktrace(boolean includeStacktrace) {
            this.includeStacktrace = includeStacktrace;
        }

        /**
         * Returns the tags that must be present for an event to be logged.
         *
         * @return the included logging tags
         */
        public Set<String> getIncludeTags() {
            return this.includeTags;
        }

        /**
         * Sets the tags that must be present for an event to be logged.
         *
         * @param includeTags the included logging tags
         */
        public void setIncludeTags(Set<String> includeTags) {
            this.includeTags = (includeTags != null ? includeTags : new LinkedHashSet<>());
        }

        /**
         * Returns the tags that suppress event logging when present.
         *
         * @return the excluded logging tags
         */
        public Set<String> getExcludeTags() {
            return this.excludeTags;
        }

        /**
         * Sets the tags that suppress event logging when present.
         *
         * @param excludeTags the excluded logging tags
         */
        public void setExcludeTags(Set<String> excludeTags) {
            this.excludeTags = (excludeTags != null ? excludeTags : new LinkedHashSet<>());
        }
    }

    /**
     * Metrics properties for the Micrometer scheduled audit listener.
     */
    public static class Metrics {

        /**
         * Whether the Micrometer metrics listener is enabled.
         */
        private boolean enabled = false;

        /**
         * Creates metrics properties with default values.
         */
        public Metrics() {
        }

        /**
         * Returns whether the Micrometer metrics listener is enabled.
         *
         * @return {@code true} when the Micrometer metrics listener is enabled
         */
        public boolean isEnabled() {
            return this.enabled;
        }

        /**
         * Sets whether the Micrometer metrics listener is enabled.
         *
         * @param enabled whether the Micrometer metrics listener is enabled
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * Defines which scheduled methods should emit audit events.
     */
    public enum Scope {
        /**
         * Audit every Spring {@code @Scheduled} method.
         */
        ALL,
        /**
         * Audit only Spring {@code @Scheduled} methods that also declare {@link ScheduledAudit}.
         */
        ANNOTATED
    }

    /**
     * Defines whether scheduler identifiers are optional or required for scheduled methods.
     */
    public enum SchedulerIdPolicy {
        /**
         * Allows scheduled methods without {@link ScheduledAudit}; events for those methods have no scheduler ID.
         */
        OPTIONAL,
        /**
         * Requires every Spring {@code @Scheduled} method to declare {@link ScheduledAudit#schedulerId()}.
         */
        REQUIRED
    }
}
