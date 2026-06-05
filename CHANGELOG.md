# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added

### Changed

### Fixed

## [2.1.0] - 2026-06-05

### Added

- Added optional Micrometer metrics integration with automatic `MeterRegistry` publishing.

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
