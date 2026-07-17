# ADR-001: Generate Layer 1 from the official spec, models-only

**Date:** 2026-07-16
**Status:** Accepted

## Context

Allegro publishes an official OpenAPI 3.0 spec (267 operations). openapi-generator can emit
models only, or models plus a full HTTP client. The house exemplar (ksef-java-sdk, its
ADR-001) generates from spec rather than wrapping any vendor SDK.

## Decision

Vendor the spec into `allegro-rest-models/openapi/` and generate **models + supporting files
only** (`java` generator, `native` library → Jackson): `*Raw` DTOs, no generated `*Api`
classes. Transport, auth, retry, and pagination are hand-written in `allegro-client`.
The spec file is frozen during a fan-out wave; refreshes are core-owner PRs between waves.

## Consequences

- 1200+ DTO classes maintained by regeneration, never by hand; cached as a JAR.
- The SDK controls every wire concern (vendor media types, retry, token lifecycle) instead of
  inheriting a generated client's abstractions.
- Composed (oneOf) DTOs require the generator's support classes — the invoker package is
  qualified-exported to the SDK module only.
