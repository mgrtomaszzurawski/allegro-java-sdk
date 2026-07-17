# ADR-008: Shared flock-guarded token store for the sandbox accounts

**Date:** 2026-07-17
**Status:** Accepted

## Context

All agents share ONE sandbox seller (and one buyer) account, and Allegro ROTATES refresh
tokens on every refresh (ADR-003). If each agent refreshed independently, every refresh would
invalidate the token every other agent holds — agents would destroy each other's sessions in
ways that look like random auth failures (pre-mortem risk B1).

## Decision

One shared token store: `/workspace/shared/.allegro-sandbox-tokens.json` (mode 600, outside
every git repo), written only under an exclusive `flock`. Demo runs READ the current access
token (12 h validity); only the `auth-bootstrap` scenario — or a due-refresh holding the
lock — performs the rotating refresh and rewrites the store. Unit tests never touch live
auth (WireMock). Credentials themselves stay in `/workspace/shared/allegro-sandbox.env`
(operator decision 2026-07-16).

## Consequences

- Exactly one process rotates at a time; everyone else free-rides on the cached token.
- A crashed writer self-heals on the next bootstrap run.
- Production consumers are unaffected — this is agent-infrastructure, not SDK behaviour;
  the SDK's own persistence hook is `AllegroClient.refreshToken()`.
