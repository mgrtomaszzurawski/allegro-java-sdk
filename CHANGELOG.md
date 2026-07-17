# Changelog

All notable changes to this project will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)
(pre-1.0: breaking changes may occur between 0.x releases).

Every merged PR appends its entries ONLY inside its own pre-created bucket subsection
under `[Unreleased]` below — never in another bucket's subsection, never reordering
sections. Empty subsections are dropped by the release engineer when folding
`[Unreleased]` into a version section at tag time.

## [Unreleased]

### Core

- Gradle reactor scaffold; Layer-1 `*Raw` DTO generation from the vendored
  official OpenAPI spec (267 operations).
- OAuth2 lifecycle (client-credentials / authorization-code / device grants,
  proactive refresh, rotation, single-flight, 401 replay); transport with
  vendor media types, equal-jitter retry, typed `errors[]` mapping; lazy
  Stream pagination; remediation-grouped exception hierarchy.
- `AllegroClient` entry point with `user().me()` vertical slice.
- Observability (AWS-SDK model): named SLF4J debug channels
  (`request`/`auth`/`retry`), `traceId()` on every exception (wire-verified
  `trace-id` header), `AllegroExecutionInterceptor` SPI with per-attempt
  callbacks.
- JaCoCo coverage gate active in `check` (85%/84% at activation); 39-test
  WireMock suite per TESTING.md.
- `allegro-demo` probe runner (`auth-bootstrap`, `me`) + flock-guarded shared
  token store (ADR-008); live sandbox verification caught and fixed a
  JsonNullable deserialization defect on `/me`.

### A — offers-core
### B — orders-payments
### C — shipping
### D — account-meta
### E — catalog-products
### F — offers-extras
### G — pricing
### H — campaigns
### I — fulfillment
### J — post-sale-comms
### K — sale-settings
