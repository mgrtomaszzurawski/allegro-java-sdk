# ADR-011: `@since` tags are provisional until the first release

**Date:** 2026-07-26
**Status:** Accepted

## Context

Public API elements carry Javadoc `@since` tags with values spanning `0.1.0`–`0.7.0`, but the
artifact version is `0.1.0-preview` and there are **no git tags** — the SDK has made no Maven
Central release. Every `@since` value therefore names a version that has not shipped. Read
literally, `@since 0.4.0` claims "released in 0.4.0", which is false, and left unmanaged it
would become wrong Javadoc the moment the first release goes out (everything present at that
point ships in the *same* first version, not the spread of versions the tags imply).

The tags do carry information — the rough development order in which elements landed on
`develop` — but that order is informal and not consistent enough to be a contract (e.g. some
early-merged core types carry a higher `@since` than later-merged ones).

## Decision

Until the first Maven Central release, `@since` tags are **provisional and not authoritative**:

- They record the approximate development wave in which an element was added on `develop`; they
  are **not** a released-version guarantee and reviewers do not gate on them matching
  `gradle.properties`.
- New public API added during the pre-release phase may carry the current in-flight `@since`
  value; do not invent a version ahead of the actual work.
- At the first release (`develop`→`main`), a single normalization pass rewrites **all** pre-release
  `@since` values to the actual first released version (the `gradle.properties` version at tag
  time). From the first tagged release onward, `@since` becomes authoritative and follows normal
  semantics (the version in which the element first shipped).

A mass retag now is deliberately **not** done: nothing has shipped, so the only correct post-release
value is "the first release version", which is unknown until release — normalizing at the release
boundary is the single point where it can be set correctly and once.

## Consequences

- Pre-release Javadoc `@since` is a soft ordering hint, not a release claim; no PR is blocked on it.
- Exactly one mechanical normalization at GA makes every `@since` correct and consistent.
- The versioning story is explicit, so the spread of `0.1.0`–`0.7.0` tags is a known, tracked state
  rather than silent drift.
