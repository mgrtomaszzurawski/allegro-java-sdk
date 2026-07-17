# ADR-003: Sealed credential hierarchy; SDK owns the whole OAuth2 lifecycle

**Date:** 2026-07-16
**Status:** Accepted

## Context

Allegro supports three OAuth2 grants (client-credentials, authorization-code, device) with
Bearer JWTs, 12h expiry, and refresh-token ROTATION on every refresh. The operator's design
contract: the consumer authenticates once; multi-step flows and refresh are SDK business.

## Decision

`sealed interface AllegroCredentials permits ClientCredentials, AuthorizationCodeCredentials,
DeviceCodeCredentials` — the consumer picks the grant by picking the type. Internal
`OAuth2TokenManager`: lazy first acquisition, cached access token, proactive refresh (60s
margin), single-flight refresh under a lock, refresh-token rotation, RFC 8628 device polling
(prompt via consumer callback), refresh→initial-grant fallback, and an `invalidate()` hook
driving the transport's single-attempt 401 replay. `AllegroClient.refreshToken()` exposes the
rotated token for session persistence.

## Consequences

- No token, header, or flow detail in any public signature; `toString()` on credentials
  redacts secrets; exception bodies pass JWT redaction.
- Concurrent callers cannot stampede the token endpoint (single-flight).
- Rotation makes concurrent refresh across PROCESSES destructive — see ADR-008.
