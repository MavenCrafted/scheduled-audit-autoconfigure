package io.github.mavencrafted.scheduling.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduledAuditTest {

    @Test
    void supportsMethodLevelRuntimeTags() throws NoSuchMethodException {
        Method method = SampleScheduledTasks.class.getDeclaredMethod("taggedTask");
        ScheduledAudit annotation = method.getAnnotation(ScheduledAudit.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.schedulerId()).isEqualTo("ACCOUNT_CLEANUP");
        assertThat(annotation.tags()).containsExactly("billing", "noisy");
    }

    @Test
    void declaresMethodTargetAndRuntimeRetention() {
        Target target = ScheduledAudit.class.getAnnotation(Target.class);
        Retention retention = ScheduledAudit.class.getAnnotation(Retention.class);

        assertThat(target).isNotNull();
        assertThat(target.value()).containsExactly(ElementType.METHOD);
        assertThat(retention).isNotNull();
        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
        assertThat(ScheduledAudit.class.isAnnotationPresent(Documented.class)).isTrue();
    }

    private static final class SampleScheduledTasks {

        @ScheduledAudit(schedulerId = "ACCOUNT_CLEANUP", tags = {"billing", "noisy"})
        void taggedTask() {
        }
    }
}
