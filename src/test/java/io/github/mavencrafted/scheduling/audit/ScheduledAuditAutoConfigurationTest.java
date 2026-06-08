package io.github.mavencrafted.scheduling.audit;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.Scheduled;

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
                        .hasSingleBean(ScheduledAuditSchedulerIdValidator.class)
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
                        "scheduled-audit.logging.exclude-tags[0]=noisy",
                        "scheduled-audit.logging.include-stacktrace=true"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    ScheduledAuditProperties properties = context.getBean(ScheduledAuditProperties.class);

                    assertThat(properties.getLogging().isIncludeStacktrace()).isTrue();
                    assertThat(properties.getLogging().getIncludeTags()).containsExactly("billing", "ops");
                    assertThat(properties.getLogging().getExcludeTags()).containsExactly("noisy");
                });
    }

    @Test
    void contextBindsAuditScope() {
        contextRunner.withPropertyValues("scheduled-audit.scope=annotated")
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    ScheduledAuditProperties properties = context.getBean(ScheduledAuditProperties.class);

                    assertThat(properties.getScope()).isEqualTo(ScheduledAuditProperties.Scope.ANNOTATED);
                });
    }

    @Test
    void contextDefaultsAuditScopeToAll() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            ScheduledAuditProperties properties = context.getBean(ScheduledAuditProperties.class);

            assertThat(properties.getScope()).isEqualTo(ScheduledAuditProperties.Scope.ALL);
        });
    }

    @Test
    void contextDefaultsFailedLoggingStacktraceToFalse() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            ScheduledAuditProperties properties = context.getBean(ScheduledAuditProperties.class);

            assertThat(properties.getLogging().isIncludeStacktrace()).isFalse();
        });
    }

    @Test
    void contextFailsWhenSchedulerIdsAreDuplicated() {
        contextRunner.withBean("firstScheduledBean", FirstDuplicateScheduledBean.class, FirstDuplicateScheduledBean::new)
                .withBean("secondScheduledBean", SecondDuplicateScheduledBean.class, SecondDuplicateScheduledBean::new)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("Duplicate scheduled audit schedulerId 'ACCOUNT_CLEANUP'")
                            .hasMessageContaining("FirstDuplicateScheduledBean.run")
                            .hasMessageContaining("SecondDuplicateScheduledBean.run");
                });
    }

    @Test
    void contextDoesNotCreateMicrometerListenerWhenMetricsPropertyIsMissing() {
        contextRunner.withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .run(context -> {
                    assertThat(context).hasNotFailed()
                            .doesNotHaveBean(MicrometerScheduledAuditListener.class);

                    assertThat(context.getBeansOfType(ScheduledAuditListener.class))
                            .containsKey("loggingScheduledAuditListener")
                            .hasSize(1);
                });
    }

    @Test
    void contextDoesNotCreateMicrometerListenerWhenMetricsAreDisabled() {
        contextRunner.withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withPropertyValues("scheduled-audit.metrics.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed()
                            .doesNotHaveBean(MicrometerScheduledAuditListener.class);

                    assertThat(context.getBeansOfType(ScheduledAuditListener.class))
                            .containsKey("loggingScheduledAuditListener")
                            .hasSize(1);
                });
    }

    @Test
    void contextCreatesMicrometerListenerWhenMetricsAreEnabledAndMeterRegistryExists() {
        contextRunner.withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withPropertyValues("scheduled-audit.metrics.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed()
                            .hasSingleBean(MeterRegistry.class)
                            .hasSingleBean(MicrometerScheduledAuditListener.class)
                            .hasSingleBean(LoggingScheduledAuditListener.class);

                    assertThat(context.getBeansOfType(ScheduledAuditListener.class))
                            .containsKeys("loggingScheduledAuditListener", "micrometerScheduledAuditListener")
                            .hasSize(2);
                });
    }

    @Test
    void contextFailsWhenMetricsAreEnabledAndMeterRegistryIsMissing() {
        contextRunner.withPropertyValues("scheduled-audit.metrics.enabled=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(UnsatisfiedDependencyException.class)
                            .hasMessageContaining("MeterRegistry");
                });
    }

    @Test
    void contextUsesCustomMicrometerListenerWhenPresent() {
        contextRunner.withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withBean("customMicrometerListener", MicrometerScheduledAuditListener.class,
                        () -> new MicrometerScheduledAuditListener(new SimpleMeterRegistry()))
                .withPropertyValues("scheduled-audit.metrics.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed()
                            .hasSingleBean(MicrometerScheduledAuditListener.class)
                            .hasSingleBean(LoggingScheduledAuditListener.class);

                    assertThat(context.getBeansOfType(ScheduledAuditListener.class))
                            .containsKeys("loggingScheduledAuditListener", "customMicrometerListener")
                            .doesNotContainKey("micrometerScheduledAuditListener")
                            .hasSize(2);
                });
    }

    static final class FirstDuplicateScheduledBean {

        @Scheduled(fixedRate = 1000)
        @ScheduledAudit(schedulerId = "ACCOUNT_CLEANUP")
        void run() {
        }
    }

    static final class SecondDuplicateScheduledBean {

        @Scheduled(fixedRate = 1000)
        @ScheduledAudit(schedulerId = "ACCOUNT_CLEANUP")
        void run() {
        }
    }
}
