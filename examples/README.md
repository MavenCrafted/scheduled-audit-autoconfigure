# Examples

## scheduled-audit-demo

[`scheduled-audit-demo`](scheduled-audit-demo) is a small Spring Boot application that shows the library in a runnable app.

It includes two audited scheduled jobs:

- `daily-report-generation`, a successful audited scheduled job
- `invoice-sync`, an intentionally failing audited scheduled job for observing `FAILED` events

It also includes `SimpleLoggingScheduledAuditListener`, a custom listener that logs execution IDs, timing, tags, and failures.

From the repository root:

```sh
./mvnw install
cd examples/scheduled-audit-demo
../../mvnw spring-boot:run
```

The example dependency version should match the root project version. CI checks this and compiles the example after installing the local library artifact.
