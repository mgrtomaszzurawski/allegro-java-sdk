# ADR-007: 11 scope-aligned buckets with exclusive ownership and append points

**Date:** 2026-07-16
**Status:** Accepted (operator decision — full fan-out, 11 parallel agents)

## Context

267 operations must be built by parallel agents without merge chaos. Considered: 1 endpoint
per agent (267 PR ceremonies, model collisions inside features, degrades facades into 1:1
endpoint mirrors) and 1 tag per agent (51 wildly uneven tags). Details:
`/workspace/shared/context/TASK-DIVISION-PLAN.md`.

## Decision

11 functionality buckets aligned with OAuth scope families (A offers-core … K sale-settings);
each bucket owned by exactly ONE agent: exclusive packages
(`sdk.domain.<feature>`, `sdk.internal.client.<feature>`), tests, fixtures dir, README
section. Shared files reduced to FOUR append-only points: module-info exports, AllegroClient
accessor+wiring, ApiPaths section, and the Offers root interface (bucket F appends its
sub-accessor lines). Core runtime + build files are FROZEN for domain agents (changes =
BACKLOG item to the core owner). Shared value types (Money, ids) live in `sdk.core` — a
bucket never mints a private copy. Every bucket's first PR is a starter slice (one endpoint
end-to-end) to calibrate conventions cheaply.

## Consequences

- Zero package overlap between agents; conflicts confined to mechanical block appends.
- Cross-cutting evolution (core, shared types, spec refresh) serializes through the core
  owner by design.
