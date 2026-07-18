# Known Allegro server behaviours

Server behaviours consumers will hit at runtime that are **not SDK bugs** and are not always
reflected in the official documentation. Discovered through the live-sandbox exploration
passes (see `TESTING.md` §2). The SDK either handles them transparently (where noted) or
surfaces them as typed exceptions so consumers can react.

Every entry states: what happens, where it was observed (endpoint + date), and how the SDK
handles it. Entries are added by the bucket owner who hit the behaviour; do not remove
entries without re-verifying on the sandbox.

## Errors and diagnostics

### Every error response carries a `trace-id` header (verified 2026-07-17, sandbox)

Error responses include a `trace-id` HTTP header (e.g. `4631702648f0524e`) alongside the
structured `errors[]` body. The SDK captures it into every `AllegroException`
(`traceId()`) — quote it in Allegro support tickets.

### `/me` with an app-only token returns 500, not 403 (verified 2026-07-17, sandbox)

`GET /me` called with a client-credentials (application) token returns
`500 INTERNAL_SERVER_ERROR` with `userMessage: "Internal server error. Try again in a few
minutes."` — NOT a 401/403 explaining the missing user context. Consumers see
`AllegroServerException` and a misleading "try again" message. The SDK cannot distinguish
this from a real server error; the `UserAccount.me()` javadoc documents that a user-context
token is required.

## Catalogue (bucket E)

### Category reads work with an app-only token (verified 2026-07-17, sandbox)

`GET /sale/categories`, `GET /sale/categories?parent.id=…` and `GET /sale/categories/{id}`
all succeed with a client-credentials (application) token — no user context needed. The live
probe returned 13 root categories, `parent.id=5` → 7 children, and `get(1520)` →
`Budownictwo i Akcesoria` (`leaf=false`, `options` present). This confirms bucket E's
category surface is exercisable with the shared seller app credentials alone.

### `CategoryDto` shape confirmed present (verified 2026-07-17, sandbox)

The spec declares no `required` fields on `CategoryDto`, but on the wire `id`, `name` and
`leaf` always arrive, `options` is present on both leaf and non-leaf categories, and `parent`
is present on children / absent on roots. The SDK's `Category` record therefore treats
`id`/`name`/`leaf` as always-present and `parentId`/`options` as nullable.

## Shipping & delivery (bucket C)

### `deliveryMethods().paymentPolicy` is a closed typed enum (spec-derived)

`GET /sale/delivery-methods` returns each method's `paymentPolicy` as one of a
fixed set (`IN_ADVANCE`, `CASH_ON_DELIVERY`). In the generated Layer-1 model this
field is a typed enumeration whose Jackson creator **rejects** any other value,
so — unlike the free-form string enums on a point of service, which fall back to
an `UNKNOWN` sentinel — a `paymentPolicy` value Allegro might add in future would
fail deserialization of the whole response (surfaced as `AllegroServerException`)
rather than mapping to a sentinel. The SDK's `PaymentPolicy` is modelled closed to
match. If Allegro extends the set, the fix is a Layer-1 regeneration (open-enum),
not a domain change. To be confirmed live once the sandbox seller token is
restored; `deliveryMethods()` is expected to work with an application (no-scope)
token.

## Account & meta (bucket D)

### Rating and CPS-conversion lists carry no `totalCount` (spec-derived, pending live verification)

`GET /sale/user-ratings` (`UserRatingListResponse`) and `GET /affiliate/conversions/cps`
(`CpsConversionResponse`) return a bare array (`ratings` / `conversions`) with no
`count`/`totalCount` field — unlike the offer/order collections. The SDK therefore paginates
these streams by the full-page heuristic: it keeps requesting pages until one comes back
shorter than the requested `limit`. Cost: when the total is an exact multiple of the page
size, one extra empty request is made at the boundary. `user-ratings` additionally caps
`offset` at 20000 (spec), so a stream over an account with more than ~20100 ratings will 400
on the page past the cap. To be confirmed live once a valid seller token is restored.

### CPS conversion prices are nullable leaves (spec-derived)

On `CpsConversion`, the `offer.unitPrice`, `commission.publisher` and `commission.allegro`
objects may be present while their `amount`/`currency` are absent. The SDK maps such an
incomplete price to a `null` `Money` rather than failing the stream.

## From external sources (to verify on first contact)

- **Sandbox seller accounts may require team-side activation** before the first offer
  listing (`VALIDATION_ERROR` / `SellerAccountVerified`); activation is granted on request in
  the allegro-api GitHub forum. Source: allegro-api issue #5101. To be verified by bucket A's
  starter slice.
- **Sandbox Sales Center UI can lag behind the API** (orders placed but not visible in the
  web UI). Trust the API view. Source: allegro-api issue #9778.
