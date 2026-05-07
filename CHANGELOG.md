# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added

### Changed

### Fixed

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
