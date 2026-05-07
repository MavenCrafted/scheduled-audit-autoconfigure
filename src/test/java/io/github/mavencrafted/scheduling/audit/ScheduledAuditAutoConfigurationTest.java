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
                        .hasSingleBean(ScheduledAuditProperties.class)
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
    void contextAddsCustomListenerAlongsideDefaultLoggingListener() {
        contextRunner.withBean("customListener", ScheduledAuditListener.class, () -> event -> { })
                .run(context -> {
                    assertThat(context).hasNotFailed()
                            .hasSingleBean(ScheduledAuditAspect.class)
                            .hasSingleBean(LoggingScheduledAuditListener.class);
                    assertThat(context.getBeansOfType(ScheduledAuditListener.class))
                            .containsKeys("customListener", "loggingScheduledAuditListener")
                            .hasSize(2);
                });
    }

    @Test
    void contextSupportsMultipleCustomListeners() {
        contextRunner.withBean("firstListener", ScheduledAuditListener.class, () -> event -> { })
                .withBean("secondListener", ScheduledAuditListener.class, () -> event -> { })
                .run(context -> {
                    assertThat(context).hasNotFailed()
                            .hasSingleBean(ScheduledAuditAspect.class)
                            .hasSingleBean(LoggingScheduledAuditListener.class);
                    assertThat(context.getBeansOfType(ScheduledAuditListener.class))
                            .containsKeys("firstListener", "secondListener", "loggingScheduledAuditListener")
                            .hasSize(3);
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

    @Test
    void contextCanDisableDefaultLoggingListener() {
        contextRunner.withPropertyValues("scheduled-audit.logging.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed()
                            .hasSingleBean(ScheduledAuditProperties.class)
                            .hasSingleBean(ScheduledAuditAspect.class)
                            .doesNotHaveBean(LoggingScheduledAuditListener.class);
                    assertThat(context.getBeansOfType(ScheduledAuditListener.class)).isEmpty();
                });
    }

    @Test
    void contextBindsLoggingTagFilters() {
        contextRunner.withPropertyValues(
                        "scheduled-audit.logging.include-tags[0]=billing",
                        "scheduled-audit.logging.include-tags[1]=ops",
                        "scheduled-audit.logging.exclude-tags[0]=noisy"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    ScheduledAuditProperties properties = context.getBean(ScheduledAuditProperties.class);

                    assertThat(properties.getLogging().getIncludeTags()).containsExactly("billing", "ops");
                    assertThat(properties.getLogging().getExcludeTags()).containsExactly("noisy");
                });
    }
}
