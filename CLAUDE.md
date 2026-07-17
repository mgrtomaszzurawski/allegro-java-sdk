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
- Branch from `develop`; PR to `develop`; squash-merge keep-branch. Never commit to
  `main`/`develop` directly. `/review` gates every PR (after open + before merge).
- No 1–3 char names. Exceptions (the EXACT list enforced by the PMD
  `ShortVariableWithDomainExceptions` rule — keep both in sync): `i`/`j`/`k` loops,
  `e`/`ex` catch, and the domain abbreviations `raw`, `ean`, `sku`, `vat`, `id`, `url`,
  `uri`, `zip`, `key`, `xml`, `seq`. No magic numbers/strings — `private static final`
  constants.
- Spec files under `allegro-rest-models/openapi/` must NEVER appear in a diff. Check
  `git diff --name-only` before committing.
- Javadoc every public API whose name doesn't carry the meaning.

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
- that feature's README section

Shared APPEND POINTS — append-only edits, one small block per bucket, rebase on `develop`
before opening the PR:
1. `allegro-client/src/main/java/module-info.java` — your `exports sdk.domain.<feature>...` lines
2. `AllegroClient` — your accessor (`public <Feature> <feature>()`) + field + wiring
3. `ApiPaths` — your feature's path-constant section

FROZEN for domain agents: `sdk/internal/runtime/**`, `sdk/{config,core,exception}` root types,
build files, quality configs. A needed core change = BACKLOG item for the core owner, never a
drive-by edit in a domain PR.

## Test patterns

- WireMock for ALL HTTP tests; VERIFY writes/retries (`WireMock.verify(N, ...)`) —
  `assertDoesNotThrow` without verify is false-green. Fixtures in
  `src/test/resources/__files/`.
- Naming `methodUnderTest_whenScenario_expectedResult`; given/when/then markers.
- Per-builder round-trip tests (`requiredFieldsOnly`, `allCoreFieldsSet`, `toBuilder_preserves`).
- Prove a new test fails without the fix. No `@Disabled` to ship. No live Allegro calls in tests;
  live verification happens via `allegro-demo` against the sandbox.

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
Playwright buyer-bot (`tools/buyer-bot/`, experiment) seeds web-only flows — see
`ARCHITECTURE.md` §10.

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
