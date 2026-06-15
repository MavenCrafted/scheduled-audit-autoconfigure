# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

## [2.3.1] - 2026-06-15

### Added

- Added `scheduled-audit.scheduler-id-policy=required` to fail startup unless
  every Spring `@Scheduled` method declares `@ScheduledAudit(schedulerId = "...")`.
- Added a runnable scheduled audit demo application under `examples/scheduled-audit-demo`.
- Added support policy documentation.
- Added production usage guidance, compatibility information, and a scheduling flowchart to the README.
- Added release supply-chain security controls:
  - CycloneDX SBOM generation for release artifacts
  - GitHub artifact provenance attestations for published JAR and SBOM files
  - Dependency review for pull requests
  - GitHub Actions pinned to immutable commit SHAs
  - Reproducible build timestamp configuration

### Changed

- Updated Spring Boot dependency management to 3.5.15.
- Updated Maven Failsafe Plugin to 3.5.6.
- Removed Mockito from the test dependency graph.
- CI now tests and builds on Java 17, 21, and 25.
- CI now compiles the example application.

## [2.2.1] - 2026-06-10

### Fixed

- Scheduled audit scheduler ID validation no longer instantiates lazy beans during application startup.

## [2.2.0] - 2026-06-09

### Added

- Added `scheduled-audit.scope` to choose whether all Spring `@Scheduled` methods are audited or only methods annotated with `@ScheduledAudit`.
- Added Spring Boot configuration metadata for IDE completion of `scheduled-audit` properties.
- Added Maven Wrapper.
- Added CI and CodeQL GitHub Actions workflows.

### Changed

- Release workflows now use the Maven Wrapper.

### Fixed

- Blank `schedulerId` values on `@ScheduledAudit` now fail application startup.
- Duplicate `schedulerId` values are now detected across scheduled bean instances, not only across different methods.

## [2.1.0] - 2026-06-05

### Added

- Added optional Micrometer metrics integration with automatic `MeterRegistry` publishing.

### Changed

- `@ScheduledAudit` now requires an explicit `schedulerId` value.

## [2.0.0] - 2026-05-08

### Added

- Added `@ScheduledAudit` for optional scheduled-job metadata.
- Added `schedulerId` support on emitted `ScheduledAuditEvent` instances.
- Added audit tags on `ScheduledAuditEvent` instances.
- Added `ScheduledAuditEvent#hasTag(String)` for listener-side filtering.
- Added startup validation that fails fast when two scheduled methods declare the same non-empty `schedulerId`.
- Added configurable default logging filters:
  - `scheduled-audit.logging.include-tags`
  - `scheduled-audit.logging.exclude-tags`
- Added `scheduled-audit.logging.include-stacktrace` to control whether failed scheduled executions log the full exception stack trace.

### Changed

- Renamed scheduled event terminology from `taskName` to `scheduledMethod`.
- Changed default failed-event logging to include failure type and message without a stack trace by default.
- Made auto-configuration infrastructure internal where applications should not construct or replace it directly.

### Removed

- Removed `ScheduledAuditEvent#getTaskName()`; use `ScheduledAuditEvent#getScheduledMethod()` instead.
- Removed public `ScheduledAuditEvent.started(...)`, `ScheduledAuditEvent.succeeded(...)`, and `ScheduledAuditEvent.failed(...)` factory methods.
- Removed public access to the default logging listener and audit aspect implementation classes.

### Migration

- Replace `event.getTaskName()` calls with `event.getScheduledMethod()`.
- Use `ScheduledAuditListener` beans to consume audit events instead of constructing `ScheduledAuditEvent` directly.
- Add `@ScheduledAudit(schedulerId = "...", tags = {...})` only when business identifiers or tag-based filtering are needed.
- Review duplicate `schedulerId` values before upgrading; duplicates now fail application startup.

## [1.0.0] - 2026-05-06

### Added

- Initial release of `scheduled-audit-autoconfigure`.
- Auto-configuration for auditing Spring `@Scheduled` method executions.
- `ScheduledAuditEvent` lifecycle events for started, succeeded, and failed executions.
- `ScheduledAuditListener` extension point for custom audit handling.
- Default logging listener for scheduled audit events.
- Support for multiple audit listeners.
- Configuration properties:
  - `scheduled-audit.enabled`
  - `scheduled-audit.logging.enabled`
- Maven Central publication.
