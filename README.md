# Scheduled Audit Auto-Configuration

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mavencrafted/scheduled-audit-autoconfigure?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.mavencrafted/scheduled-audit-autoconfigure/overview)
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
    <version>2.0.0</version>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

`scheduled-audit-autoconfigure` is enabled automatically when it is present in a Spring Boot application. Existing `@Scheduled` methods are intercepted without additional setup, and the default logging listener can be observed through debug logging.

## Audit Metadata

Scheduled methods can declare optional business metadata with `@ScheduledAudit`:

```java
@Scheduled(fixedDelay = 600000)
@ScheduledAudit(schedulerId = "ACCOUNT_CLEANUP", tags = {"billing", "maintenance"})
public void cleanUpAccounts() {
    // scheduled work
}
```

The emitted `scheduledMethod` uses the fully qualified scheduled method name, for example `io.github.example.AccountCleanupJob.cleanUpAccounts`.
When `@ScheduledAudit` is present, events also carry the optional `schedulerId` and tags. Declared `schedulerId` values must be unique across scheduled methods; duplicate values fail application startup so audit records remain unambiguous.

## Custom Listeners

Custom listeners can be added by declaring one or more `ScheduledAuditListener` beans:

```java
@Bean
ScheduledAuditListener databaseScheduledAuditListener(
        ScheduledAuditRepository repository) {

    return event -> repository.save(
            ScheduledAuditEntity.from(event));
}
```
```java
@Bean
ScheduledAuditListener kafkaScheduledAuditListener(
        KafkaTemplate<String, ScheduledAuditEvent> kafka) {

    return event -> kafka.send(
            "scheduled-audit-events",
            event.executionId(),
            event);
}
```

When multiple listeners are present, each listener receives every scheduled audit event. The default logging listener remains enabled unless `scheduled-audit.logging.enabled=false` is configured. If one listener fails, the remaining listeners are still invoked and the scheduled job execution is not interrupted.

## Configuration

```yaml
scheduled-audit:
  enabled: true
  logging:
    enabled: true
    include-stacktrace: false
    include-tags: []
    exclude-tags: []

logging:
  level:
    io.github.mavencrafted: DEBUG
```

With this configuration, the default logging listener writes `STARTED` and `SUCCEEDED` events at `DEBUG` level and `FAILED` events at `ERROR` level:

```text
Scheduled task started [executionId=8f8df9b2-c84c-4a54-a6a4-7cd0f9c0f6ee, scheduledMethod=io.github.example.AccountCleanupJob.run, startedAt=2026-05-06T19:22:00.012974Z]
Scheduled task succeeded [executionId=8f8df9b2-c84c-4a54-a6a4-7cd0f9c0f6ee, scheduledMethod=io.github.example.AccountCleanupJob.run, startedAt=2026-05-06T19:22:00.012974Z, finishedAt=2026-05-06T19:22:10.017384Z, duration=PT10.00441S]
Scheduled task failed [executionId=3fceebec-f3f9-4acb-bcb7-dc78ac4c8b8b, scheduledMethod=io.github.example.AccountCleanupJob.run, startedAt=2026-05-06T19:22:30.028913Z, finishedAt=2026-05-06T19:22:40.037654Z, duration=PT10.008741S, failureType=java.lang.IllegalStateException, failureMessage=Scheduled task failed]
```

Set `scheduled-audit.logging.include-stacktrace=true` to include the full failure stack trace in the default logger.

Use `scheduled-audit.logging.include-tags` to log only events that have at least one configured tag. Use `scheduled-audit.logging.exclude-tags` to suppress events with matching tags; excluded tags take precedence over included tags.

## Configuration Properties

| Property | Default | Description |
| --- | --- | --- |
| `scheduled-audit.enabled` | `true` | Enables scheduled audit auto-configuration. |
| `scheduled-audit.logging.enabled` | `true` | Enables the default logging listener. |
| `scheduled-audit.logging.include-stacktrace` | `false` | Includes the thrown exception stack trace for failed scheduled executions. |
| `scheduled-audit.logging.include-tags` | empty | Logs only events with at least one matching tag when configured. |
| `scheduled-audit.logging.exclude-tags` | empty | Suppresses events with matching tags. Takes precedence over `include-tags`. |

## Migration from 1.x

Version `2.0.0` intentionally renames task-oriented event API to scheduled-method terminology. Consumers that handled `ScheduledAuditEvent` directly should replace `getTaskName()` with `getScheduledMethod()`. Event construction helpers are now internal because events are emitted by the auto-configuration; application code should consume events through `ScheduledAuditListener`.

## Repository Maintenance

This repository is maintained with consistent branch and commit conventions:

- Branch format: `<type>/<scope>-<description>`
- Commit format: `type(scope): description`
- Supported types: `feat`, `fix`, `chore`, `refactor`, `docs`, `test`
