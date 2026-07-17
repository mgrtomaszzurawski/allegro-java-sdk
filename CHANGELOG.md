# Changelog

All notable changes to this project will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)
(pre-1.0: breaking changes may occur between 0.x releases).

Every merged PR appends its entries under `[Unreleased]` in its own bucket
subsection (append-only; bucket letter order). The release engineer folds
`[Unreleased]` into a version section at tag time.

## [Unreleased]

### Core

- Gradle reactor scaffold; Layer-1 `*Raw` DTO generation from the vendored
  official OpenAPI spec (267 operations).
- OAuth2 lifecycle (client-credentials / authorization-code / device grants,
  proactive refresh, rotation, single-flight, 401 replay); transport with
  vendor media types, equal-jitter retry, typed `errors[]` mapping; lazy
  Stream pagination; remediation-grouped exception hierarchy.
- `AllegroClient` entry point with `user().me()` vertical slice.
