package io.github.mavencrafted.scheduling.audit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduledAuditAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(ScheduledAuditAutoConfiguration.class));

    @Test
    void contextStartsWithDefaultScheduledAuditBeans() {
        contextRunner.run(context ->
                assertThat(context).hasNotFailed()
                        .hasSingleBean(ScheduledAuditAspect.class)
                        .hasSingleBean(ScheduledAuditListener.class)
        );
    }

    @Test
    void contextUsesLoggingListenerByDefault() {
        contextRunner.run(context ->
                assertThat(context.getBean(ScheduledAuditListener.class))
                        .isInstanceOf(LoggingScheduledAuditListener.class)
        );
    }

    @Test
    void contextBacksOffDefaultListenerWhenCustomListenerIsPresent() {
        contextRunner.withBean(ScheduledAuditListener.class, () -> event -> { })
                .run(context ->
                        assertThat(context).hasSingleBean(ScheduledAuditListener.class)
                                .doesNotHaveBean(LoggingScheduledAuditListener.class)
                );
    }

    @Test
    void contextSupportsMultipleCustomListeners() {
        contextRunner.withBean("firstListener", ScheduledAuditListener.class, () -> event -> { })
                .withBean("secondListener", ScheduledAuditListener.class, () -> event -> { })
                .run(context -> {
                    assertThat(context).hasNotFailed()
                            .hasSingleBean(ScheduledAuditAspect.class)
                            .doesNotHaveBean(LoggingScheduledAuditListener.class);
                    assertThat(context.getBeansOfType(ScheduledAuditListener.class))
                            .containsOnlyKeys("firstListener", "secondListener");
                });
    }

    @Test
    void contextDoesNotLoadWhenEnabledPropertyIsFalse() {
        contextRunner.withPropertyValues("scheduled-audit.enabled=false")
                .run(context ->
                        assertThat(context).doesNotHaveBean(ScheduledAuditAspect.class)
                                .doesNotHaveBean(ScheduledAuditListener.class)
                );
    }
}
