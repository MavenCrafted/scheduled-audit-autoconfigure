package io.github.mavencrafted.scheduling.audit;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduledAuditAutoConfigurationImportsTest {

    @Test
    void autoConfigurationImportsFileExists() throws Exception {
        var resource = new ClassPathResource(
                "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"
        );

        assertThat(resource.exists())
                .isTrue();
        assertThat(resource.getContentAsString(StandardCharsets.UTF_8))
                .contains(ScheduledAuditAutoConfiguration.class.getName());
    }
}
