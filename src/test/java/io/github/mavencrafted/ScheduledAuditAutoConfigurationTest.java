package io.github.mavencrafted;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduledAuditAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(ScheduledAuditAutoConfiguration.class));

    @Test
    void contextStartsWithAutoConfiguration() {
        contextRunner.run(context ->
                assertThat(context).hasNotFailed()
        );
    }

    @Test
    void contextStartsWhenEnabledPropertyIsTrue() {
        contextRunner.withPropertyValues("scheduled-audit.enabled=true")
                .run(context ->
                        assertThat(context).hasNotFailed()
                );
    }

    @Test
    void contextStartsWhenEnabledPropertyIsFalse() {
        contextRunner.withPropertyValues("scheduled-audit.enabled=false")
                .run(context ->
                        assertThat(context).hasNotFailed()
                );
    }
}
