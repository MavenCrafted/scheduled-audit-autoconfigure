# Scheduled Audit Auto-Configuration

[![javadoc](https://javadoc.io/badge2/io.github.mavencrafted/scheduled-audit-autoconfigure/javadoc.svg)](https://javadoc.io/doc/io.github.mavencrafted/scheduled-audit-autoconfigure)
[![CodeQL](https://github.com/MavenCrafted/scheduled-audit-autoconfigure/actions/workflows/github-code-scanning/codeql/badge.svg)](https://github.com/MavenCrafted/scheduled-audit-autoconfigure/actions/workflows/github-code-scanning/codeql)

`scheduled-audit-autoconfigure` is a Spring Boot auto-configuration module for publishing audit events around `@Scheduled` job executions.

It provides a listener-based extension point for scheduled job lifecycle events such as start, completion, and failure, and includes a default logging listener out of the box.

## Installation

To use the library, add `scheduled-audit-autoconfigure` together with `spring-boot-starter-aop`:

```xml
<dependency>
    <groupId>io.github.mavencrafted</groupId>
    <artifactId>scheduled-audit-autoconfigure</artifactId>
    <version>1.0.0</version>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

`scheduled-audit-autoconfigure` is enabled automatically when it is present in a Spring Boot application. Existing `@Scheduled` methods are intercepted without additional setup, and the default logging listener can be observed through debug logging.

Custom listeners can be added by declaring one or more `ScheduledAuditListener` beans:

```java
@Bean
ScheduledAuditListener databaseScheduledAuditListener() {
    return event -> {
        // handle the audit event
    };
}
```

When multiple listeners are present, each listener receives every scheduled audit event. The default logging listener remains enabled unless `scheduled-audit.logging.enabled=false` is configured. If one listener fails, the remaining listeners are still invoked and the scheduled job execution is not interrupted.

```yaml
scheduled-audit:
  enabled: true
  logging:
    enabled: true

logging:
  level:
    io.github.mavencrafted: DEBUG
```

With this configuration, the default logging listener writes `STARTED` and `SUCCEEDED` events at `DEBUG` level and `FAILED` events at `ERROR` level:

```text
Scheduled task started [executionId=8f8df9b2-c84c-4a54-a6a4-7cd0f9c0f6ee, taskName=io.github.example.AccountCleanupJob.run, startedAt=2026-05-06T19:22:00.012974Z]
Scheduled task succeeded [executionId=8f8df9b2-c84c-4a54-a6a4-7cd0f9c0f6ee, taskName=io.github.example.AccountCleanupJob.run, startedAt=2026-05-06T19:22:00.012974Z, finishedAt=2026-05-06T19:22:10.017384Z, duration=PT10.00441S]
Scheduled task failed [executionId=3fceebec-f3f9-4acb-bcb7-dc78ac4c8b8b, taskName=io.github.example.AccountCleanupJob.run, startedAt=2026-05-06T19:22:30.028913Z, finishedAt=2026-05-06T19:22:40.037654Z, duration=PT10.008741S]
java.lang.IllegalStateException: Scheduled task failed
```

The emitted `taskName` uses the fully qualified scheduled method name, for example `io.github.example.AccountCleanupJob.run`.

## Repository Maintenance

This repository is maintained with consistent branch and commit conventions:

- Branch format: `<type>/<scope>-<description>`
- Commit format: `type(scope): description`
- Supported types: `feat`, `fix`, `chore`, `refactor`, `docs`, `test`
