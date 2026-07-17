# ADR-004: Exceptions grouped by remediation, not HTTP status

**Date:** 2026-07-16
**Status:** Accepted

## Context

Fold/split test: "what does the consumer do if they catch THIS that they wouldn't do for its
parent?" Same answer → fold into one type. Allegro returns a structured `errors[]` payload.

## Decision

Seven unchecked types under `AllegroException`: BadRequest (typed `AllegroFieldError` list
parsed from `errors[]`), Auth, RateLimit (`retryAfterSeconds`), NotFound, Server (5xx +
network + timeout as ONE type), AsyncTimeout (may have succeeded server-side), Config
(fail-fast at construction). Mapping happens once, in the transport boundary
(`ServerErrorParser`).

## Consequences

- Consumers write catch blocks that encode decisions, not status-code trivia.
- 422 folds into BadRequest; 403 folds into Auth — no per-status types.
