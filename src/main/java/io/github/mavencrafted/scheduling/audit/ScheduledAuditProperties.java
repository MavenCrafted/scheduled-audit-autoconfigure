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
     * Logging configuration for the default scheduled audit listener.
     */
    private final Logging logging = new Logging();

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
     * Returns the logging properties for the default scheduled audit listener.
     *
     * @return the logging properties
     */
    public Logging getLogging() {
        return this.logging;
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
}
