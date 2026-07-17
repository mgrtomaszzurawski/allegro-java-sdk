# Allegro Java SDK — Architecture

Typed, OpenAPI-first Java SDK for the Allegro REST API. This document describes the target
architecture accepted in the task-division plan (2026-07-16) and the parts already built.
Companion documents: `CLAUDE.md` (binding conventions), `README.md` (consumer quick start).

## 1. Design goals

1. **Intent-named API.** Methods say what they DO, not which endpoint they call:
   `offers.publish(...)`, `orders.confirm(...)` — never `postOfferPublicationCommand`.
2. **Authenticate once.** The consumer passes credentials a single time; every OAuth2 step —
   token acquisition, caching, proactive refresh, refresh-token rotation, device-flow
   verification, 401 recovery — is SDK-internal and invisible.
3. **Full typing.** Every request and response is a typed, immutable Java record; requests are
   assembled with fluent builders that validate required fields fail-fast. No maps, no raw JSON.
4. **Nothing generated leaks.** Generated DTOs and internal plumbing are JPMS-hidden; the
   consumer sees only the `sdk.*` exported surface.
5. **Resilient by default.** Retry with equal-jitter backoff, `Retry-After`-aware 429 handling,
   lazy streaming pagination — all on defaults tuned for Allegro's documented limits.

## 2. Module reactor (Gradle, Java 17, JPMS)

| Module | JPMS name | Published | Role |
|---|---|---|---|
| `allegro-rest-models` | `io.github.mgrtomaszzurawski.allegro.rest` | yes | Layer 1: `*Raw` DTOs generated from the vendored official OpenAPI 3.0 spec (`openapi/allegro-openapi.yaml`, 267 operations). Models + Jackson support only; no generated HTTP client. Qualified export **only to** the SDK module; `opens` to Jackson. |
| `allegro-client` | `io.github.mgrtomaszzurawski.allegro` | yes | Layers 2–3: all hand-written SDK code (this document). |
| `allegro-demo` | — | no | Live sandbox probe runner (manual execution). |
| `allegro-examples` | — | no | Compile-only check of README/consumer snippets. |
| `allegro-jpms-consumer` | `…allegro.jpms.consumer` | no | Named-module compile gate: if a public type ever lands in a non-exported package, this module stops compiling. |

## 3. Three layers

```
┌────────────────────────────────────────────────────────────────────┐
│ Consumer                                                           │
│   try (var client = AllegroClient.create(credentials, SANDBOX)) {  │
│       CurrentUser me = client.user().me();                         │
│   }                                                                │
├────────────────────────────────────────────────────────────────────┤
│ Layer 3 — public API (JPMS-exported)                               │
│   sdk/AllegroClient          single AutoCloseable entry point      │
│   sdk/domain/<feature>/      Facade interface + builder/ + model/  │
│   sdk/config[.credentials,.policy], sdk/exception                  │
├────────────────────────────────────────────────────────────────────┤
│ Layer 2 — internal (NOT exported)                                  │
│   sdk/internal/client/<feature>/   *Impl endpoint wrappers +       │
│                                    package-private mappers         │
│   sdk/internal/runtime/transport/  HttpRuntime, HttpSupport,       │
│                                    RetryHandler, ApiPaths,         │
│                                    ServerErrorParser               │
│   sdk/internal/runtime/auth/       OAuth2TokenManager              │
│   sdk/internal/runtime/pagination/ PagedSpliterator                │
├────────────────────────────────────────────────────────────────────┤
│ Layer 1 — generated (separate module, qualified export)            │
│   client/model/*Raw   mutable Jackson POJOs, regenerated at build  │
└────────────────────────────────────────────────────────────────────┘
```

Rules that keep the layers honest:

- Consumers import only `sdk.*` exported packages — never `internal.*`, never `*Raw`.
- Public methods return immutable records created via `static X from(XRaw raw)` factories;
  domain clients map `*Raw` → record at the boundary, in package-private mappers.
- `HttpRuntime` is a narrow interface so the transport never imports the facade
  (one-directional layering).
- Spec `required:true` is trusted — no defensive `!= null` on required fields; a violated
  server contract fails loudly.

## 4. Authentication (OAuth2, all three Allegro grants)

Credentials are a **sealed hierarchy** — the consumer picks the grant by picking the type:

```java
sealed interface AllegroCredentials
        permits ClientCredentials,           // app-only token (public data)
                AuthorizationCodeCredentials, // browser flow: code+redirectUri XOR refreshToken
                DeviceCodeCredentials         // console flow: userPrompt callback (+ refreshToken)
```

`OAuth2TokenManager` (internal) owns the lifecycle:

```
requireToken() ── cached & fresh? ──────────────────────────► return token
      │ no (or < 60s to expiry)
      ▼ single-flight (concurrent callers wait)
  refresh token held? ── yes ─► grant_type=refresh_token ─► rotate stored refresh token
      │ no / refresh rejected
      ▼
  by credential type:
    ClientCredentials      → grant_type=client_credentials
    AuthorizationCode      → exchange one-time code (redirect_uri validated)
    DeviceCode             → POST /device → userPrompt(verificationUri) → poll
                             (authorization_pending / slow_down per RFC 8628)
```

- HTTP 401 mid-call → transport invalidates the cache and replays **once**; a second 401
  surfaces as `AllegroAuthException`.
- `AllegroClient.refreshToken()` exposes the current (rotated) refresh token so applications
  persist the session and skip future prompts.
- Token material is never logged; exception bodies pass through JWT redaction
  (`safeResponseBody()`).

## 5. Transport pipeline

Every domain call goes through `HttpSupport`:

```
facade → *Impl → HttpSupport.{get,postJson,putJson,delete}Authenticated
           │  Accept/Content-Type: application/vnd.allegro.public.v1+json  (never consumer-visible)
           │  Authorization: Bearer <OAuth2TokenManager.requireToken()>
           ▼
       RetryHandler.send()          equal-jitter backoff (base 500ms, ×2/attempt),
           │                        retryOn5xx + network, retryOn429 honouring
           │                        Retry-After (capped), retryPost=false default
           ▼
       401? → reauthenticate, replay once
       non-2xx? → ServerErrorParser → typed exception (see §7)
       2xx → Jackson → *Raw → from(raw) → immutable record
```

## 6. Pagination

Offset/limit is the dominant Allegro idiom (`limit ≤ 1000`, `offset+limit ≤ 10M`,
`count`/`totalCount` in responses). List operations return **lazy `Stream<T>`** via
`PagedSpliterator` — one page in heap, next page fetched on demand, defensive cap on
consecutive empty pages. A cursor variant covers marker-paged resources (e.g. checkout-form
event streams). There is deliberately no `listAll()`.

## 7. Exceptions — grouped by remediation, not HTTP status

| Exception | Meaning | Consumer remediation |
|---|---|---|
| `AllegroBadRequestException` | 400/422, carries `List<AllegroFieldError>` parsed from `errors[]` (code, message, userMessage, path, details) | fix the request |
| `AllegroAuthException` | 401/403, failed token acquisition/refresh, missing scope | fix credentials / re-authorize |
| `AllegroRateLimitException` | 429 after retries, `retryAfterSeconds()` | slow down |
| `AllegroNotFoundException` | 404 on a concrete resource | verify the identifier |
| `AllegroServerException` | 5xx + network + timeout (one type: same remediation) | retry later / alert |
| `AllegroAsyncTimeoutException` | internal polling gave up; may have succeeded server-side | check state before resubmit |
| `AllegroConfigException` | invalid configuration | fix config (thrown at construction) |

All unchecked, all extending `AllegroException` (statusCode + raw/safe response body).

## 8. Async command endpoints — sync by default

Allegro models bulk writes as command queues (submit → poll status). The SDK wraps them
**sync-default**: one intent-named method submits and polls internally until a terminal
status, with an optional `Duration` timeout overload. No `CompletableFuture` in the public
surface — thread-pool choices belong to the consumer.

## 9. Domain map (Phase 1 fan-out)

11 buckets aligned with OAuth scope families; each bucket = one facade family owned by exactly
one agent (assignment in the shared BACKLOG):

| Facade area | Packages under `sdk.domain.` | Scope family |
|---|---|---|
| A offers-core | `offers` | `sale:offers:*` |
| B orders-payments | `orders`, `payments` | `orders:*`, `payments:*`, `billing:read` |
| C shipping | `shipping` | `shipments:*` |
| D account-meta | `account` *(me() shipped with core)* | `profile:*`, `ratings`, `bids` |
| E catalog-products | `catalog` | `sale:offers:read` |
| F offers-extras | `offers.tags`, `offers.bundles`, `classifieds` | `sale:offers:*` |
| G pricing | `pricing` | `sale:offers:*`, `billing:read` |
| H campaigns | `campaigns` | `campaigns` |
| I fulfillment | `fulfillment` | `fulfillment:*` |
| J post-sale-comms | `disputes`, `messaging` | `disputes`, `messaging` |
| K sale-settings | `settings` | `sale:settings:*` |

Parallel-work contract: each bucket owns its `sdk/domain/<feature>/**` +
`sdk/internal/client/<feature>/**` + tests exclusively; the only shared files are three
**append points** (`module-info.java` exports, `AllegroClient` accessor+wiring, `ApiPaths`
section — marked with `[append point: …]` comments); the core runtime is frozen for domain
agents. Full rules: `TASK-DIVISION-PLAN.md` + repo `CLAUDE.md`.

**Shared value types live in `sdk.core`, never duplicated per bucket.** Cross-domain concepts
the spec reuses everywhere — money amounts (`{amount, currency}`), marketplace ids, seller
references — get ONE record in `sdk.core`, owned by the core owner. A bucket that needs a new
shared type files a BACKLOG item instead of minting a private copy; eleven agents each
inventing their own `Price` record would hand consumers eleven incompatible types for the
same concept.

## 10. Quality architecture

Gates are layered by cost: cheap ones run on every commit, the full battery once per PR, the
expensive ones at release boundaries.

### 10.1 Static gates (every `check`)

Spotless (license headers) → Checkstyle → PMD (incl. the custom short-name rule synced with
`CLAUDE.md`) → SpotBugs → JUnit + JaCoCo report. **Coverage gate** (bundle INSTRUCTION ≥ 0.75
/ METHOD ≥ 0.80; per-class METHOD = 1.00 on every `domain.*.builder.*Builder` and
`domain.*.*Client`) attaches to `check` with the first domain PR. **JPMS gates:** hand-written
module descriptors + the `allegro-jpms-consumer` compile gate.

### 10.2 Tests

WireMock for ALL HTTP behaviour — happy paths, typed error mapping (400 `errors[]`, 401
re-auth replay, 429 retry), with `WireMock.verify(...)` on writes and retries (an
`assertDoesNotThrow` without verify is false-green). Per-builder round-trip tests
(`requiredFieldsOnly`, `allCoreFieldsSet`, `toBuilder_preserves`). Fixture shapes come from
the official Postman collection / live sandbox captures, not from imagination — the spec
declares types, the wire decides reality. No live Allegro calls in unit tests.

### 10.3 SonarQube (PR-ready, once per PR)

`./gradlew sonar --no-configuration-cache` after a full build, against the project's Sonar
server (`sonar.projectKey=allegro-java-sdk`; coverage from the JaCoCo XML; demo/examples/
jpms-consumer and generated trees excluded from analysis). Pass bar: 0 bugs, 0
vulnerabilities, no new smells on touched files. A PR is not merge-ready before its Sonar
pass is green.

### 10.4 Mutation testing — PIT (adequacy oracle, report-only)

Coverage proves a line executed; PIT proves an assertion is load-bearing. Run at the
`develop` → `main` boundary (release candidate), scoped to hand-written `allegro-client`
code — never per-PR (slow; equivalent mutants make 100% unreachable). Target band
**~70–85% mutation score**; surviving mutants in the token manager, retry handler, or error
parser are treated as missing tests and fixed before a release tag. Report-only: the build
never fails on the score, the release engineer reads the report.

### 10.5 `allegro-demo` — live sandbox probe runner

The only place real HTTP happens. A manually-invoked runner
(`./gradlew :allegro-demo:run -Pdemo.scenario=<name>`) executing named scenarios against the
sandbox with credentials sourced from the environment (never tracked):

- `auth-bootstrap` — device-flow bootstrap for the seller and buyer accounts; persists rotated
  refresh tokens to the shared token store so every later run (and every agent) starts
  non-interactively.
- `me` — the Phase 0.5 vertical proof: full stack auth → transport → mapping on live sandbox.
- One scenario per domain bucket lands with that bucket's PR (e.g. `offers-lifecycle`,
  `orders-read`, `messaging-roundtrip`) — the bucket's DoD includes its demo scenario passing
  against the sandbox.

Demo scenarios are wire-touching verification, not tests: they run on demand, log
status-codes only, and never run in CI or `check`.

### 10.6 Playwright buyer-bot (user simulation for web-only flows)

The public API has no buyer-side checkout: buy-now purchases, opening disputes/returns as a
buyer, and leaving ratings happen only in the web UI (verified against Allegro docs + team
answers). A Node 20 + Playwright bot under `tools/buyer-bot/` (not a Gradle module, not
published) simulates the buyer on the **sandbox web UI**: log in as the buyer account, buy a
listed offer with the simulated sandbox payment, optionally open a dispute — seeding real
orders/disputes that seller-side demo scenarios then exercise via the API. It can also click
through device-flow consent screens, making token bootstrap fully non-interactive.

Status: EXPERIMENT — Allegro fronts with DataDome-class anti-bot protection that may block
headless browsers even on sandbox; a probe decides. Fallback: one-time manual purchases in
the sandbox UI (orders persist and remain queryable indefinitely). API-reachable buyer
actions (auction bidding, Message Center both directions) don't need the bot at all — they
use a buyer user-token via the SDK itself.

### 10.7 Process

Feature branch → PR to `develop` → `/review` gate (after open + again before merge) →
squash-merge keep-branch. OWASP `dependencyCheckAggregate` (CVSS ≥ 7 fails) + PIT at release
boundaries only.

## 11. Environments

| | Production | Sandbox |
|---|---|---|
| API | `https://api.allegro.pl` | `https://api.allegro.pl.allegrosandbox.pl` |
| OAuth | `https://allegro.pl/auth/oauth/*` | `https://allegro.pl.allegrosandbox.pl/auth/oauth/*` |

Selected via `AllegroEnvironment`; both carry the same rate limits (~9000 req/min per client
id + per-user leaky buckets → 429 with `Retry-After`).

## 12. Status

- **Built:** reactor + Layer-1 generation (PR #1, merged); config/credentials/policy,
  exception hierarchy, pagination, OAuth2 token manager, transport pipeline, `AllegroClient`
  entry point, and the `user().me()` vertical slice (branch `feature/core-runtime`).
- **In progress:** WireMock test suite, JaCoCo gate activation, live sandbox demo.
- **Next:** Phase 1 domain fan-out per the accepted plan.
