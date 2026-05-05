# Scheduled Audit Auto-Configuration

`scheduled-audit-autoconfigure` is a Spring Boot library for auditing the execution of `@Scheduled` jobs.

It is designed to capture scheduled job lifecycle events such as start, completion, failure, and duration, with support for audit backends implemented through logging or persistence.

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

`scheduled-audit-autoconfigure` is enabled automatically when it is present in a Spring Boot application. Existing `@Scheduled` methods are picked up without additional setup, and debug logging can be used to observe scheduled job audit activity.

## Repository Maintenance

This repository is maintained with consistent branch and commit conventions:

- Branch format: `<type>/<scope>-<description>`
- Commit format: `type(scope): description`
- Supported types: `feat`, `fix`, `chore`, `refactor`, `docs`, `test`
