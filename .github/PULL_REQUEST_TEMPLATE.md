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
- [ ] fixtures carry provenance (no unreviewed `spec-derived` marks in a
      bucket-final PR)
- [ ] demo scenario for this bucket ran green against the sandbox
      (`./gradlew :allegro-demo:run -Pdemo.scenario=<bucket>`) — REQUIRED
      for bucket-final PRs; note new KNOWN-SERVER-BEHAVIORS entries
- [ ] Sonar pass green at PR-ready
-->

## Backwards compatibility

<!--
Pre-1.0: breaking changes between 0.x releases are allowed - note them in
CHANGELOG under [Unreleased]. No back-compat shims.
-->

## Notes for reviewer

<!-- Optional: non-obvious decisions, alternatives considered, deferrals. -->
