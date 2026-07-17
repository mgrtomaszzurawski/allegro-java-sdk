# ADR-002: Three-layer architecture, JPMS, Java 17 baseline

**Date:** 2026-07-16
**Status:** Accepted

## Context

The SDK must guarantee that generated types and internal plumbing never leak to consumers,
on a Java 17 LTS baseline (pattern-switch on sealed types is 21+ — not available).

## Decision

Layer 3 `sdk.*` (exported: client, config, credentials, policy, core, exception,
domain.<feature>.*) / Layer 2 `sdk.internal.*` (never exported) / Layer 1 generated module
(qualified export to the SDK module only). Named modules everywhere;
`allegro-jpms-consumer` is a compile gate over the exported surface. Sealed-hierarchy
dispatch uses instanceof patterns (Java 17-legal), not switch patterns.

## Consequences

- A public type in a non-exported package breaks the build, not a consumer.
- `@NullMarked` package-info on every exported package; internals stay unannotated.
- Baseline bump to 21 (switch patterns, HttpClient.close()) is a future ADR.
