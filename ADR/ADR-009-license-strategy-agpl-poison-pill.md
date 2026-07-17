# ADR-009: AGPL-3.0-only as a deliberate poison pill with commercial dual-licensing

**Date:** 2026-07-17
**Status:** Accepted (operator decision)

## Context

The repo is public (portfolio visibility; recruiters can read the code) while the Allegro
integration domain is inherently commercial — any serious consumer embeds the SDK in a
seller backend or SaaS. A permissive license (Apache-2.0/MIT) would maximize adoption; the
operator explicitly does NOT want adoption, external contributors, or a community project.

## Decision

License stays **AGPL-3.0-only**, on purpose:

- The network-copyleft clause makes commercial closed-source use effectively impossible —
  the code is readable by everyone, usable in practice only by the copyright holder.
- The operator uses the SDK in their own products (the copyright holder is not bound by
  their own license).
- If a buyer ever appears, the operator grants a separate commercial license
  (dual-licensing). This requires keeping the copyright ownership clean: external
  contributions are NOT accepted unless accompanied by a copyright assignment.

Repo operations that follow: default branch is `develop` (day-to-day work and contribution
history); `main` carries ONLY full releases published to Maven Central, each with an
annotated `v<version>` tag.

## Consequences

- No CONTRIBUTING.md inviting patches; PRs from outside are closed with a policy note.
- Relicensing stays a one-party decision for as long as all code is operator-copyright.
- Maven Central publications carry the AGPL pom exactly as configured.
