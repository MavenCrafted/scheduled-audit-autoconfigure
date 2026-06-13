package io.github.mavencrafted.scheduling.audit;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.atomic.AtomicBoolean;

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
    void contextBindsSchedulerIdPolicy() {
        contextRunner.withPropertyValues("scheduled-audit.scheduler-id-policy=required")
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    ScheduledAuditProperties properties = context.getBean(ScheduledAuditProperties.class);

                    assertThat(properties.getSchedulerIdPolicy())
                            .isEqualTo(ScheduledAuditProperties.SchedulerIdPolicy.REQUIRED);
                });
    }

    @Test
    void contextBindsMetricsEnabled() {
        contextRunner.withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withPropertyValues("scheduled-audit.metrics.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    ScheduledAuditProperties properties = context.getBean(ScheduledAuditProperties.class);

                    assertThat(properties.getMetrics().isEnabled()).isTrue();
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
    void contextDefaultsSchedulerIdPolicyToOptional() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            ScheduledAuditProperties properties = context.getBean(ScheduledAuditProperties.class);

            assertThat(properties.getSchedulerIdPolicy())
                    .isEqualTo(ScheduledAuditProperties.SchedulerIdPolicy.OPTIONAL);
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
                            .hasMessageContaining("firstScheduledBean:")
                            .hasMessageContaining("FirstDuplicateScheduledBean.run")
                            .hasMessageContaining("secondScheduledBean:")
                            .hasMessageContaining("SecondDuplicateScheduledBean.run");
                });
    }

    @Test
    void contextFailsWhenSchedulerIdsAreDuplicatedAcrossBeanInstances() {
        contextRunner.withBean("firstDuplicateBeanInstance", DuplicateBeanInstanceScheduledBean.class,
                        DuplicateBeanInstanceScheduledBean::new)
                .withBean("secondDuplicateBeanInstance", DuplicateBeanInstanceScheduledBean.class,
                        DuplicateBeanInstanceScheduledBean::new)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("Duplicate scheduled audit schedulerId 'ACCOUNT_CLEANUP'")
                            .hasMessageContaining("firstDuplicateBeanInstance:")
                            .hasMessageContaining("secondDuplicateBeanInstance:")
                            .hasMessageContaining("DuplicateBeanInstanceScheduledBean.run");
                });
    }

    @Test
    void contextFailsWhenSchedulerIdIsBlank() {
        contextRunner.withBean("blankSchedulerIdScheduledBean", BlankSchedulerIdScheduledBean.class,
                        BlankSchedulerIdScheduledBean::new)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("Blank scheduled audit schedulerId")
                            .hasMessageContaining("blankSchedulerIdScheduledBean:")
                            .hasMessageContaining("BlankSchedulerIdScheduledBean.run");
                });
    }

    @Test
    void contextAllowsScheduledMethodsWithoutSchedulerIdWhenSchedulerIdPolicyIsOptional() {
        contextRunner.withBean("plainScheduledBean", PlainScheduledBean.class, PlainScheduledBean::new)
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void contextFailsWhenSchedulerIdPolicyIsRequiredAndScheduledAuditIsMissing() {
        contextRunner.withPropertyValues("scheduled-audit.scheduler-id-policy=required")
                .withBean("plainScheduledBean", PlainScheduledBean.class, PlainScheduledBean::new)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("Missing scheduled audit schedulerId")
                            .hasMessageContaining("scheduled-audit.scheduler-id-policy=required")
                            .hasMessageContaining("plainScheduledBean:")
                            .hasMessageContaining("PlainScheduledBean.run");
                });
    }

    @Test
    void contextValidatesRequiredSchedulerIdPolicyOnLazyScheduledBeansWithoutInstantiatingThem() {
        AtomicBoolean lazyBeanInstantiated = new AtomicBoolean();

        contextRunner.withPropertyValues("scheduled-audit.scheduler-id-policy=required")
                .withBean("lazyPlainScheduledBean", LazyPlainScheduledBean.class,
                        () -> {
                            lazyBeanInstantiated.set(true);
                            return new LazyPlainScheduledBean();
                        },
                        beanDefinition -> beanDefinition.setLazyInit(true))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(lazyBeanInstantiated).isFalse();
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("Missing scheduled audit schedulerId")
                            .hasMessageContaining("lazyPlainScheduledBean:")
                            .hasMessageContaining("LazyPlainScheduledBean.run");
                });
    }

    @Test
    void contextValidatesLazyScheduledBeansWithoutInstantiatingThem() {
        AtomicBoolean lazyBeanInstantiated = new AtomicBoolean();

        contextRunner.withBean("lazyBlankSchedulerIdScheduledBean", LazyBlankSchedulerIdScheduledBean.class,
                        () -> {
                            lazyBeanInstantiated.set(true);
                            return new LazyBlankSchedulerIdScheduledBean();
                        },
                        beanDefinition -> beanDefinition.setLazyInit(true))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(lazyBeanInstantiated).isFalse();
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("Blank scheduled audit schedulerId")
                            .hasMessageContaining("lazyBlankSchedulerIdScheduledBean:")
                            .hasMessageContaining("LazyBlankSchedulerIdScheduledBean.run");
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

    static final class DuplicateBeanInstanceScheduledBean {

        @Scheduled(fixedRate = 1000)
        @ScheduledAudit(schedulerId = "ACCOUNT_CLEANUP")
        void run() {
        }
    }

    static final class PlainScheduledBean {

        @Scheduled(fixedRate = 1000)
        void run() {
        }
    }

    static final class LazyPlainScheduledBean {

        @Scheduled(fixedRate = 1000)
        void run() {
        }
    }

    static final class BlankSchedulerIdScheduledBean {

        @Scheduled(fixedRate = 1000)
        @ScheduledAudit(tags = {"BLABLA"}, schedulerId = " ")
        void run() {
        }
    }

    static final class LazyBlankSchedulerIdScheduledBean {

        @Scheduled(fixedRate = 1000)
        @ScheduledAudit(schedulerId = " ")
        void run() {
        }
    }

}
