# ADR-005: Async command endpoints are wrapped sync-default

**Date:** 2026-07-16
**Status:** Accepted

## Context

Allegro models bulk writes as command queues (submit a command, poll its status/tasks):
offer publication, price/quantity changes, promo options, Allegro Prices, AlleDiscount, ASN
submit. A 1:1 mapping would force every consumer to hand-roll polling.

## Decision

One intent-named method per command (`offers().batch().publish(...)`) submits AND polls
internally to a terminal state, returning the final report. Optional `Duration timeout`
overload; on expiry → `AllegroAsyncTimeoutException` (explicitly documents the operation may
still succeed server-side). Polling stops on ANY terminal status, not only success. No
`CompletableFuture` in the public surface — thread choices belong to consumers. Command-status
endpoints are consumed internally and never appear as public methods (~25 endpoints removed
from the surface; see API-SURFACE.md).

## Consequences

- The consumer's mental model is "call → result", matching AWS/Azure SDK ergonomics.
- Long-running commands occupy a caller thread by design; documented per method.
