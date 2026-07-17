# Known Allegro server behaviours

Server behaviours consumers will hit at runtime that are **not SDK bugs** and are not always
reflected in the official documentation. Discovered through the live-sandbox exploration
passes (see `TESTING.md` §2). The SDK either handles them transparently (where noted) or
surfaces them as typed exceptions so consumers can react.

Every entry states: what happens, where it was observed (endpoint + date), and how the SDK
handles it. Entries are added by the bucket owner who hit the behaviour; do not remove
entries without re-verifying on the sandbox.

_No verified entries yet — the first exploration passes land with the Phase 0.5 `me()`
scenario and each bucket's starter slice._

## From external sources (to verify on first contact)

- **Sandbox seller accounts may require team-side activation** before the first offer
  listing (`VALIDATION_ERROR` / `SellerAccountVerified`); activation is granted on request in
  the allegro-api GitHub forum. Source: allegro-api issue #5101. To be verified by bucket A's
  starter slice.
- **Sandbox Sales Center UI can lag behind the API** (orders placed but not visible in the
  web UI). Trust the API view. Source: allegro-api issue #9778.
