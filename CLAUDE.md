# CLAUDE.md

Repository conventions and architecture notes for assistants working on this codebase.
This file is BINDING for every agent that clones this repo. Read it before touching the tree.

## What this is

OpenAPI-first, typed Java SDK for the Allegro REST API. Multi-module Gradle project:

- `allegro-rest-models` — Layer-1 `*Raw` DTOs generated from the vendored official OpenAPI 3.0
  spec (`allegro-rest-models/openapi/allegro-openapi.yaml`). Models-only + supporting files,
  Jackson (`native` library). Never edited by hand; regenerated at build.
- `allegro-client` — the SDK library (hand-written Layers 2–3). Published artefact.
- `allegro-demo` — live sandbox probe runner (manual execution; not published).
- `allegro-examples` — compile-only check of consumer examples (not published).
- `allegro-e2e` — live E2E layer: Java tests that drive the SDK AND (via Playwright-Java,
  in-process) the buyer-side web UI for flows the REST API can't reach. `@Tag("e2e")`,
  excluded from `check`, run with `-Pe2e`; not published.
- `allegro-jpms-consumer` — JPMS consumer compile gate over the exported surface.

Coordinates: `io.github.mgrtomaszzurawski:allegro-client` (version in `gradle.properties`).
License AGPL-3.0-only. Java 17, JPMS named modules.

**Current state: bootstrap scaffold.** Only the `sdk` entry-point package exists;
`AllegroClient` is a placeholder. The transport/auth core and domain buckets land per the
task-division plan in the shared context (`/workspace/shared/context/TASK-DIVISION-PLAN.md`
+ `BACKLOG.md`). Do not start a domain bucket that is not assigned to you in the BACKLOG.

## Rules

- **All code, comments, docs, commit messages in English.** Allegro spec identifiers stay as-is.
- **No AI attribution anywhere** (commits, PRs, code, docs).
- Branch from `develop` (the DEFAULT branch); PR to `develop`; squash-merge keep-branch.
  Never commit to `main`/`develop` directly. `main` carries ONLY full Maven Central
  releases with `v<version>` tags (operator merges). `/review` gates every PR (after
  open + before merge).
- License is AGPL-3.0-only ON PURPOSE (ADR-009: poison pill + commercial dual-licensing).
  Do not accept external contributions without operator sign-off AND a copyright assignment
  (dual-licensing depends on clean single-party ownership).
- No 1–3 char names. Exceptions (the EXACT list enforced by the PMD
  `ShortVariableWithDomainExceptions` rule — keep both in sync): `i`/`j`/`k` loops,
  `e`/`ex` catch, and the domain abbreviations `raw`, `ean`, `sku`, `vat`, `id`, `url`,
  `uri`, `zip`, `key`, `xml`, `seq`. No magic numbers/strings — `private static final`
  constants.
- Spec files under `allegro-rest-models/openapi/` must NEVER appear in a diff. Check
  `git diff --name-only` before committing.
- Javadoc every public API whose name doesn't carry the meaning.
- **ADRs (`ADR/`) are immutable and BINDING** — consult before changing any design they
  cover; supersede with a new ADR, never edit a decision. Every merged PR appends its
  CHANGELOG entries ONLY inside its own pre-created bucket subsection under `[Unreleased]`
  (never touch another bucket's subsection or the section order). PRs follow
  `.github/PULL_REQUEST_TEMPLATE.md`.

## Project documents map — what to read and when

In this repo (canonical, versioned with the code):

| File | What it is | Read it when |
|---|---|---|
| `CLAUDE.md` | THIS file — binding conventions, collision rules, gates | always, before touching the tree |
| `ARCHITECTURE.md` | design: layers, OAuth lifecycle, transport, observability (AWS-model §11), quality stack | before any design/impl work |
| `API-SURFACE.md` | proposed consumer method layout for all 267 ops — your bucket's naming contract | before designing your facade |
| `TESTING.md` | BINDING test conventions + the live-layer write→read exploration rules | before writing any test |
| `KNOWN-SERVER-BEHAVIORS.md` | live-verified server quirks (dated) | before wrapping an endpoint; ADD entries from your exploration |
| `ADR/` | immutable decisions ADR-001.. — supersede, never edit | before changing anything they cover; new decision = new ADR in the same PR |
| `CHANGELOG.md` | Keep-a-Changelog; write ONLY in your bucket's subsection | every PR |
| `README.md` | consumer-facing (what/why/how to use) — NOT an agent doc | when your bucket adds its `docs/<feature>.md` + link |
| `docs/<feature>.md` | per-bucket consumer usage guide (bucket-owned) | ships with your bucket PR |
| `.github/PULL_REQUEST_TEMPLATE.md` | PR checklist (bucket, append points, TESTING.md, demo) | every PR |

On the shared volume (`/workspace/shared/`, agent coordination — never committed to this repo):
`CLAUDE.md` (common rules, credentials location) · `context/BACKLOG.md` (assignment board +
phase status — re-read before EVERY PR) · `context/TASK-DIVISION-PLAN.md` (accepted plan,
DoD) · `context/ALLEGRO-API-RESEARCH.md` (platform facts) ·
`context/RISKS-MULTIAGENT-PREMORTEM.md` (failure modes) · `context/RAG-PLAN.md` (doc corpus
+ RAG phases) · `context/REPORT-*.md` (session reports).

## Knowledge base — query FIRST, grep/Read second

The `allegro-sdk-rag` MCP (each cell registers it: `claude mcp add allegro-sdk-rag --scope
local --transport http http://host.docker.internal:8772/mcp`) indexes the API spec digests,
platform tutorials, repo docs/ADRs, and per-class code prose. **Prefer it over grep/Read/Bash
searching** for anything conceptual — endpoints, scopes, SDK method contracts
(`operation_lookup`), bucket taxonomy, "where/how is X done". It is a navigation aid, not
authority: the spec and the code decide. Tool details arrive with the MCP's own instructions.

## Build and test commands

```bash
./gradlew build                    # full reactor + all static gates
./gradlew :allegro-client:build    # SDK module only
./gradlew :allegro-client:check    # static analysis only
./gradlew :allegro-client:test --tests SomeTest
```

Save full-build logs to `/tmp/build-YYYYMMDD-HHmm.txt` and grep the file; do not re-run
multi-minute builds to search a different pattern. Per commit: compile + the focused test.
Full `build` + Sonar + `/review`: once, at PR-ready.

## Architecture (three layers + JPMS)

```
Layer 3  sdk/AllegroClient (entry point, AutoCloseable) + sdk/domain/<feature>/
         (Facade interface + builder/ + model/) + sdk/{config,core,exception}
Layer 2  sdk/internal/client/<feature>/*Impl + mapping/  — endpoint wrappers
         sdk/internal/runtime/{transport,auth,pagination} — plumbing (HttpRuntime,
         HttpSupport, RetryHandler, ApiPaths, OAuth2 token manager, PagedSpliterator)
Layer 1  allegro-rest-models: client.model.*Raw generated DTOs
```

- Consumers import ONLY `sdk.*` exported packages — never `internal.*`, never `*Raw`.
- Public methods return immutable records from `sdk.domain.<feature>.model`, created via
  `static X from(XRaw raw)` factories; requests are built by fluent builders that validate
  required fields fail-fast.
- **Methods say what they DO, not which endpoint they call.** `offers.publish(...)`,
  `orders.confirm(...)` — never `postOfferPublicationCommand`. Async command endpoints are
  wrapped sync-default (submit + poll internally, optional `Duration` timeout overload);
  no `CompletableFuture` in the public surface.
- Auth is the SDK's job: consumer passes credentials once; token acquisition, caching,
  proactive refresh, and single-attempt 401 re-auth are internal. Trust spec `required:true`
  — no defensive `!= null` on required fields.
- Transport sets the vendor media type (`application/vnd.allegro.public.v1+json`) — never
  exposed to consumers. Lazy `Stream<T>` pagination via `PagedSpliterator`; never `listAll()`.
- Exceptions grouped by remediation (bad-input w/ typed field errors from the `errors[]`
  payload; auth; rate-limit w/ `retryAfterSeconds`; server 5xx+network+timeout as one;
  not-found; config fail-fast). Never log request/response bodies or tokens.

## Parallel-agent collision rules

Each domain bucket is owned by exactly one agent (assignment in shared `BACKLOG.md`).

Exclusively owned by the bucket owner (no one else touches them):
- `sdk/domain/<feature>/**`, `sdk/internal/client/<feature>/**`
- that feature's tests + WireMock fixtures (`src/test/resources/__files/<feature>/`)
- that feature's consumer usage doc `docs/<feature>.md` (linked from README's Documentation
  list) + its CHANGELOG subsection under `[Unreleased]`

Shared APPEND POINTS — append-only edits, one small block per bucket. Before opening the
PR (and again if `develop` moved before your merge): `git merge origin/develop` into YOUR
feature branch, resolve the conflicts there, push. Squash-merge to `develop` flattens the
history, so merge-based updates are safe — never rebase-force-push a shared branch:
1. `allegro-client/src/main/java/module-info.java` — your `exports sdk.domain.<feature>...` lines
2. `AllegroClient` — your accessor (`public <Feature> <feature>()`) + field + wiring
3. `ApiPaths` — your feature's path-constant section
4. `Offers` root interface (owned by bucket A) — bucket F appends its sub-accessor lines
   (`tags()`, `translations()`, `bundles()`, `flexibleBundles()`, `rating()`) under the same
   append-only regime; see `API-SURFACE.md`.

FROZEN for domain agents: `sdk/internal/runtime/**`, `sdk/{config,core,exception}` root types,
build files, quality configs. A needed core change = BACKLOG item for the core owner, never a
drive-by edit in a domain PR.

## Test patterns

**`TESTING.md` is BINDING — read it before writing any test.** The short version:

- WireMock for ALL HTTP tests; `@WireMockTest` class per domain client; all literals as
  `TEST_*` constants; stubs pin the contract (auth header + method/path + request body);
  VERIFY writes/retries (`WireMock.verify(N, ...)`) — `assertDoesNotThrow` without verify is
  false-green. Mandatory error-path table (400/401-replay/404/429/5xx) per facade. Pagination
  tests prove laziness. Fixtures carry provenance (Postman/sandbox capture; `spec-derived`
  marks must be wire-verified before the bucket's final PR).
- Naming `methodUnderTest_whenScenario_expectedResult`; given/when/then markers.
- Per-builder round-trip tests (`requiredFieldsOnly`, `allCoreFieldsSet`, `toBuilder_preserves`)
  + one failure test per required field.
- Prove a new test fails without the fix. No `@Disabled` to ship. No live Allegro calls in
  unit tests.
- **`allegro-demo` is an exploration/verification TOOL, not a test suite**: every
  wire-touching facade area is verified on the sandbox with the write→read cycle THROUGH the
  SDK (create with POST/PUT, read back with GET, assert the round-trip) before its PR is
  merge-ready; server surprises go to `KNOWN-SERVER-BEHAVIORS.md`.
- **`allegro-e2e` is the buyer-side E2E layer** (Playwright-Java in-process): for flows whose
  E2E needs a web-only buyer action (buy-now, disputes, device-flow consent), a Java test drives
  the browser AND the SDK. `@Tag("e2e")`, excluded from `check`, run with `-Pe2e`; **serial +
  rate-limited** and reuses a `0600` `storageState` (login once) — rapid logins trip DataDome's
  IP block. See `TESTING.md` §3.

## Quality gates

Spotless (license header) → Checkstyle → PMD → SpotBugs → JUnit + JaCoCo report — all wired
into `check`. The JaCoCo coverage VERIFICATION gate (bundle INSTRUCTION ≥ 0.75 / METHOD ≥ 0.80,
per-class METHOD = 1.00 on `domain.*.builder.*Builder` + `domain.*.*Client`) is staged in
`allegro-client/build.gradle.kts` and attaches to `check` with the first domain PR.
Sonar at PR-ready: `./gradlew sonar --no-configuration-cache -Dsonar.host.url=$SONAR_HOST_URL
-Dsonar.login=$SONAR_LOGIN -Dsonar.password=$SONAR_PASSWORD` (after `build`).
Release boundary (`develop`→`main`) only: OWASP `dependencyCheckAggregate` (fails on CVSS ≥ 7)
+ **PIT mutation testing** (report-only, hand-written `allegro-client` code, target band
~70–85% — surviving mutants in token manager / retry / error parser are missing tests, fix
before tagging). Live sandbox demo scenarios (`allegro-demo`) per bucket at PR DoD; the
buyer-side web-only flows (device-flow consent click for a buyer token; buy-now/disputes)
are driven in-process by **Playwright-Java** from the `allegro-e2e` module — a Java E2E test
interleaves web actions with SDK calls and assertions. Full Chromium under Xvfb beats DataDome
(headless is blocked); logins MUST reuse a saved `storageState` (login once — a fresh login per
run from a datacenter IP trips DataDome's hard IP block). Design: `ARCHITECTURE.md` §10.6.

## Allegro API facts the code relies on

- Environments: prod `https://api.allegro.pl`, sandbox `https://api.allegro.pl.allegrosandbox.pl`
  (OAuth: `https://allegro.pl/auth/oauth/*`, sandbox `https://allegro.pl.allegrosandbox.pl/auth/oauth/*`).
- OAuth2: authorization-code, device (`/auth/oauth/device`), client-credentials; Basic auth on
  the token endpoint; Bearer JWT access tokens + refresh tokens. 21 scopes (`allegro:api:...`).
- Versioning via media type `application/vnd.allegro.public.v1+json` (beta: `vnd.allegro.beta`).
- Pagination offset/limit: `limit` ≤ 1000 (default 20), `offset+limit` ≤ 10 000 000; responses
  carry `count`/`totalCount`.
- Rate limit ~9000 req/min per client id (+ per-user leaky bucket on some resources) → 429 with
  `Retry-After`; RetryPolicy honours it capped, equal-jitter backoff, `retryPost=false` default.
- Error payload: `errors[]` of `{code,message,userMessage,path,details,metadata}`.

Full research: `/workspace/shared/context/ALLEGRO-API-RESEARCH.md`.
