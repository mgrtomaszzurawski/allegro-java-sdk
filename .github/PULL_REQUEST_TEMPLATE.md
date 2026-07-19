<!--
PR template for allegro-java-sdk.
Required sections: Summary, Bucket & scope, Test plan.
-->

## Summary

<!-- 1-3 sentences. What changes and why. Avoid restating the diff. -->

## Bucket & scope

<!--
- Bucket letter (A-K) or "core" — must match your BACKLOG assignment.
- Starter slice or full-bucket package?
- Which append points does this PR touch (module-info / AllegroClient /
  ApiPaths / Offers root / none)? Confirm you rebased on develop AFTER the
  last merge to those files.
- Endpoints covered, mapped to API-SURFACE.md methods (note any renames
  vs the proposed surface, with a one-line why).
-->

## Test plan

<!--
- [ ] `./gradlew build` green locally (full reactor, all static gates)
- [ ] WireMock tests follow TESTING.md: contract-pinning stubs, verify(N)
      on writes/retries, mandatory error-path table covered for the facade
- [ ] per-builder round-trips + per-required-field failure tests
- [ ] fixtures the changed mapping relies on carry provenance — no
      `spec-derived: not yet wire-verified` marks remain on them
- [ ] LIVE sandbox write→read ran green against the sandbox on THIS code —
      REQUIRED for EVERY wire-touching PR, not just bucket-final (TESTING.md §2);
      note new KNOWN-SERVER-BEHAVIORS entries. Sandbox unreachable = STOP and
      report, do NOT merge on WireMock alone.
- [ ] Sonar pass green at PR-ready
-->

## Backwards compatibility

<!--
Pre-1.0: breaking changes between 0.x releases are allowed - note them in
CHANGELOG under [Unreleased]. No back-compat shims.
-->

## Notes for reviewer

<!-- Optional: non-obvious decisions, alternatives considered, deferrals. -->
