package io.github.mavencrafted.scheduling.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares audit metadata for a Spring {@code @Scheduled} method.
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ScheduledAudit {

    /**
     * Returns the optional business identifier for the scheduled method.
     *
     * @return the configured scheduler identifier
     */
    String schedulerId();

    /**
     * Returns the audit tags associated with the scheduled method.
     *
     * @return the configured audit tags
     */
    String[] tags() default {};
}
