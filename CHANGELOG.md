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
- Cross-bucket transport capabilities via the fluent `HttpCall` builder: encoded
  query parameters (`Query`), PATCH, no-content/void writes, beta vendor media
  type, `Accept-Language`, conditional `If-Match`, and binary request bodies.
- Shared value type `sdk.core.Money` (exact-string amount + ISO-4217 currency).
- `allegro-demo` scenario-registration seam (`DemoScenario` + append point) so
  every bucket adds probes without colliding on a shared dispatch.
- `CommandPoller` — the sync-default engine for Allegro's asynchronous command
  endpoints (ADR-005): submit-then-poll to a terminal state with bounded
  backoff, an optional `Duration` timeout, and `AllegroAsyncTimeoutException`;
  status shape and terminal predicate supplied by the caller. Shared by
  buckets A (batch), C (WZA), H (badges/subsidies), I (ASN).
- Binary download (`HttpCall.fetchBytes`) and conditional-read
  (`HttpCall.fetchWithETag` → `Etagged<T>`): the transport response path is now
  generic over the body type, so a byte download still shares retry, 401 replay
  and typed error mapping (the error body is decoded from bytes). Unblocks
  bucket I (PDF, `If-Match`) and bucket J (attachment downloads).

### A — offers-core
### B — orders-payments
### C — shipping
### D — account-meta

- `client.marketplaces().list()` — the platform's marketplaces with their
  languages, currencies and shipping countries (`GET /marketplaces`; public,
  works with an app-only token). Bucket D starter slice.

### E — catalog-products
### F — offers-extras
### G — pricing
### H — campaigns

- `client.campaigns().badges().availableCampaigns()` — list badge campaigns available to the
  authenticated seller (GET `/sale/badge-campaigns`), with an optional per-marketplace overload.
  Immutable `BadgeCampaign` model with eligibility, refusal reasons and the application/visibility/
  publication schedules. Starter slice of bucket H.

### I — fulfillment
### J — post-sale-comms
### K — sale-settings
