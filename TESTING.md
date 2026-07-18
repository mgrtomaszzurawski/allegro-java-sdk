# TESTING.md — binding test conventions

This document is BINDING for every agent, same rank as `CLAUDE.md`. It exists because tests
written to a convention force thinking; tests written to make a bar turn green are worthless.
The convention below is the house style proven in `ksef-java-sdk` — follow it, don't reinvent it.

**The prime rule: every test must be able to fail for a reason a consumer would care about.**
A test that cannot fail (`assertDoesNotThrow` with no verify, an assertion on your own stub) is
forbidden and will be rejected in review. Prove a new test RED first: revert the code under
test (or its fix), watch the test fail, restore. No `@Disabled` ever ships.

## 1. Unit + WireMock layer (all HTTP behaviour, no live calls)

### Class shape

- One test class per domain client: `<Feature>ClientTest`, annotated `@WireMockTest`.
- ALL literals are `private static final` constants at the top (`TEST_TOKEN`,
  `TEST_OFFER_ID`, path constants, status ints). Small JSON fixtures are text blocks
  `.formatted(...)` with the constants injected; large ones live in
  `src/test/resources/__files/<feature>/` — one directory per bucket, never another bucket's.
- Shared HTTP constants (`Authorization`, `Bearer `, content types) come from the shared
  `TestHttpConstants` helper, not re-declared per class.

### Naming and structure

- Method names: `methodUnderTest_whenScenario_expectedResult` — the name states the behaviour,
  not the mechanics (`publish_whenOfferInactive_submitsPublicationCommand`, never `testPublish2`).
- Body structure: `// given`, `// when`, `// then` markers in that order (`// then`-only form
  allowed for `assertThrows`). One behaviour per test; splitting beats branching.

### Stubs pin the contract, verifies prove the wire

- Every stub matches what the SDK PROMISES to send: HTTP method + path, the
  `Authorization: Bearer <token>` header, the vendor `Accept`/`Content-Type`, and for writes
  a `withRequestBody(...)` matcher on the fields that matter.
- Every write and every retry test ends with `WireMock.verify(N, ...)` — the count is the
  assertion. A retry test that doesn't count attempts proves nothing.

### Mandatory error-path coverage (per facade, not per method)

Each domain client's test class covers, at least once against a representative endpoint:

| Wire behaviour | Expected SDK behaviour to assert |
|---|---|
| 400/422 with `errors[]` | `AllegroBadRequestException` with PARSED field errors (code, path) |
| 401 once, then 200 | transparent re-auth replay — `verify(2, …)` and the second request carries the fresh token |
| 404 | `AllegroNotFoundException` |
| 429 with `Retry-After` | retry happens (verify count), then `AllegroRateLimitException.retryAfterSeconds()` when exhausted |
| 5xx then 200 | retry succeeds; and POSTs are NOT retried by default (verify(1)) |

### Pagination tests prove laziness

A `stream…()` test must show the second page is NOT fetched until the consumer advances past
page one (`limit(pageSize)` then `verify(1, …)`), and that filters/query params survive across
page boundaries (stub page 2 with full parameter match).

### Builder round-trips (per builder, enforced by the per-class METHOD=1.00 gate)

`requiredFieldsOnly` / `allCoreFieldsSet` / `toBuilder_preserves`, plus one failure test per
required field (missing → `IllegalStateException` with the message asserted). Server-side
constraints replicated in builders (length ranges, enum sets) each get a boundary test.

### Fixture provenance — the spec lies by omission

Response fixtures come from the official Postman collection or a captured sandbox response.
A fixture written from the spec alone is marked `// spec-derived: not yet wire-verified` and
MUST be replaced (or confirmed) by the bucket's exploration pass (§2) before the bucket's
final PR. Nullability, formats, and undocumented fields are exactly where the spec and the
wire diverge.

## 2. Live sandbox layer — an EXPLORATION AND VERIFICATION TOOL, not a test suite

`allegro-demo` is not "integration tests". It is the instrument each agent uses to check the
code it wrote against reality, and to explore what the server actually does before/while
wrapping it. Green WireMock tests prove the SDK matches YOUR understanding of the API;
only the sandbox proves your understanding matches Allegro.

### The write→read rule

Wrapping a GET and calling it against docs-shaped data proves nothing. For every
wire-touching facade area the owning agent verifies the full cycle **through the SDK itself**
on the sandbox:

1. **Create state with the SDK** — POST/PUT the resource (publish the offer, send the
   message, set the setting).
2. **Read it back with the SDK** — GET/stream it and assert the round-trip: what you wrote is
   what you read (ids resolve, fields survive, enums map).
3. **Tear down / prefix** — demo-created entities carry the bucket prefix (`[A-demo] …`),
   scenarios clean up after themselves and never touch another bucket's prefix.

Read-only areas (catalogue) verify response SHAPE against the live wire instead — every field
your record maps must actually arrive non-null/parseable on a real response.

### When to run it

- **Explore BEFORE or WHILE implementing** — probing an endpoint on sandbox (via a demo
  scenario or a raw call) to see real payloads is encouraged; write down what you learn.
- **Verify at PR-ready** — a bucket's PR is not merge-ready until its demo scenario passed
  against the sandbox on the code under review. The scenario ships in the same PR
  (`./gradlew :allegro-demo:run -Pdemo.scenario=<bucket>`), so verification is repeatable.

### Where findings go

- A server surprise that consumers will hit (undocumented status, quirky validation, async
  lag) → an entry in **`KNOWN-SERVER-BEHAVIORS.md`** (dated, with how the SDK handles it).
  It is not an SDK bug; it is the wire's truth.
- A finding that invalidates your fixtures → fix the fixtures (drop the `spec-derived` mark).
- A finding that invalidates the SDK design → RCA + fix the code, never fix-by-docs.

### Discipline

- Credentials only from the environment (`/workspace/shared/secrets/allegro-sandbox.env`); token
  refresh goes through the shared token store — never refresh ad hoc (rotation kills the
  other agents' sessions; see the pre-mortem, risk B1).
- Demo scenarios log operation names and statuses, never bodies or tokens.
- Sandbox rate limits are production-grade; a scenario is a handful of calls, not a load test.

## 3. Buyer-side E2E layer — Java driving the web UI via Playwright (`allegro-e2e`)

Some SDK flows can only be verified end-to-end by performing a **web-only buyer action** — buy-now,
opening a dispute/return, leaving a rating, or clicking the device-flow consent that mints the buyer
token. Those actions have no REST API, so for a real E2E the web step is part of the test's control
flow (seller creates an offer via the SDK → buyer buys and opens a dispute in the UI → the SDK reads
and answers the dispute → assert). The test therefore **cannot exist without a browser**: it drives
Playwright's **Java binding in-process** and interleaves it with SDK calls and JUnit assertions. Home:
the `allegro-e2e` module (`BuyerBrowser`).

- **Not part of `check`.** Every E2E test is `@Tag("e2e")`; the default `test` task excludes that tag,
  so a normal build launches no browser. Opt in with `-Pe2e`. The module is not published.
- **Headed under Xvfb.** Allegro fronts with DataDome; headless is blocked, full Chromium under Xvfb
  passes (`DISPLAY=:99 ./gradlew :allegro-e2e:test -Pe2e`). Recipe lives in `BuyerBrowser` /
  `ARCHITECTURE.md` §10.6.
- **Serial + rate-limited — mandatory, not optional.** The tests share one buyer session and IP; a
  series of rapid logins trips DataDome's HARD IP block (demonstrated 2026-07-17). So the task runs
  `maxParallelForks = 1`, `forkEvery = 1`, JUnit parallelism off — one browser at a time, human-paced.
- **Log in at most once — reuse `storageState`.** `BuyerBrowser` reuses a saved `storageState` (cookies
  incl. the DataDome cookie + session) and only logs in when there is none. Bootstrap it once with
  `:allegro-e2e:run` (ideally from a clean IP). The state file lives under
  `/workspace/shared/secrets/` at `0600` (live session cookies — outside any git repo, never committed).
- **DataDome may escalate to an interactive CAPTCHA — we do NOT solve it.** Verified 2026-07-18:
  the device-flow consent URL (and a fresh login from a stale cookie) can present DataDome's
  slider/audio puzzle, not just the self-clearing interstitial. Automating an anti-bot puzzle is
  detection evasion, so `BuyerBrowser` detects it (`isDataDomeChallenge`) and fails loudly instead.
  Details + tiers: `KNOWN-SERVER-BEHAVIORS.md` (Web UI anti-bot).
- **Fallback (the buyer token is a one-time human click).** OAuth device-flow consent is by design a
  single human authorization: run `:allegro-demo:run -Pdemo.scenario=auth-bootstrap -Pdemo.account=buyer`,
  open the printed URL in a **normal browser** (residential IP → no CAPTCHA), click *Authorize* — the
  refresh token lands in the shared store and every later run is non-interactive. Fresh web-session
  automation (buy-now/dispute) from a datacenter IP is puzzle-blocked the same way; seed the session
  from a clean IP or by hand. The API-reachable buyer actions (bidding, Message Center) never need the
  browser — they use the buyer user-token through the SDK.

The three verification layers: **§1 WireMock** pins the contract and runs in `check`; **§2 the
`allegro-demo` tool** does live write→read *through the SDK*; **§3 this E2E layer** adds the web-only
buyer half that §2 cannot reach, and is the precondition/interleave for those flows.

## 4. Gates are the floor, not the goal

JaCoCo (bundle ≥ 0.75/0.80 instruction/method; per-class METHOD = 1.00 on builders and domain
clients), Sonar (0 bugs / 0 vulns / no new smells on touched files), and release-time PIT
(~70–85% mutation score) are safety nets. A gate that cannot RUN is not a gate that passed —
if Sonar or the sandbox is unreachable at PR-ready, STOP and report instead of merging.
Surviving PIT mutants in the token manager, retry handler, or error parser are treated as
missing tests, not as noise.
