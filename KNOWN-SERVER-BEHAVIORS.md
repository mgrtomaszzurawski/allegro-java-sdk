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

### Category parameters are polymorphic by `type`; suggestions nest parents (verified 2026-07-18, sandbox)

`GET /sale/categories/{id}/parameters` returns `parameters[]` whose element shape depends on
`type` (`dictionary`/`float`/`integer`/`string`): dictionary parameters carry a `dictionary[]` of
`{id,value,dependsOnValueIds}` plus `restrictions.multipleChoices`; numeric parameters carry
`restrictions.{min,max,range[,precision]}`; string parameters carry
`restrictions.{minLength,maxLength,allowedNumberOfValues}`. Live probe: category `1520`
("Budownictwo i Akcesoria") returned 7 parameters, the first ("Stan") a required dictionary with
5 values. The SDK flattens all four onto one `CategoryParameter` record (`CategoryParameterType`
+ nullable `ParameterRestrictions` + a `DictionaryValue` list). NOTE: the Layer-1 parameter DTO
declares no Jackson `defaultImpl`, so an unrecognised future `type` currently fails
deserialization (surfaced as `AllegroServerException`) rather than degrading to
`CategoryParameterType.OTHER`; delivering that degradation is a core follow-up (generator
`defaultImpl` / mapper `FAIL_ON_INVALID_SUBTYPE`).

`GET /sale/matching-categories?name=` returns `matchingCategories[]`, each a category node whose
`parent` nests recursively up to the root (`parent` absent on a root match). Live probe:
`name=iphone` returned 10 matches, the top being `353` "Etui i pokrowce" with a parent chain.
Both endpoints succeed with an app-only client-credentials token (no user context, no scope).

## Shipping & delivery (bucket C)

### `GET /sale/delivery-methods` works with an app-only token (verified 2026-07-18, sandbox)

`shipping().deliveryMethods()` succeeds with a client-credentials (application)
token — the endpoint declares no OAuth scope. The live sandbox returned **571**
delivery methods; the first mapped cleanly (`paymentPolicy=IN_ADVANCE`,
`destinationCountry=PL`, `marketplaces=[1]`), confirming the `DeliveryMethod`
record's field shape against the wire. `dispatchCountry` arrived `null` on that
method, confirming it is genuinely nullable.

### `deliveryMethods().paymentPolicy` is a closed typed enum (verified 2026-07-18, sandbox)

Each method's `paymentPolicy` is one of a fixed set (`IN_ADVANCE`,
`CASH_ON_DELIVERY`). In the generated Layer-1 model this field is a typed
enumeration whose Jackson creator **rejects** any other value, so — unlike the
free-form string enums on a point of service, which fall back to an `UNKNOWN`
sentinel — a `paymentPolicy` value Allegro might add in future would fail
deserialization of the whole response (surfaced as `AllegroServerException`)
rather than mapping to a sentinel. The SDK's `PaymentPolicy` is modelled closed to
match, and the raw→domain map is a by-name lookup guarded by a name-parity test
(`ShippingEnumsTest`) that iterates both enums. If Allegro extends the set, a
Layer-1 regeneration adds the constant and that test then fails in the build —
forcing the domain enum to gain the value in the same change, rather than leaking
a runtime error.

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

### `bidding().myBid(offerId)` returns 404 for both "no auction" and "no bid" (verified 2026-07-18, sandbox)

`GET /bidding/offers/{offerId}/bid` answers 404 whether the offer is not an auction (or does
not exist) or the auction exists but the user has placed no bid — the two cases are not
distinguished on the wire. The SDK surfaces both as `AllegroNotFoundException`. Confirmed live
with a buyer user token against a non-existent offer id (device-consent → buyer token minted by
the one-time `auth-bootstrap -Pdemo.account=buyer` flow; see the DataDome section below).

## Sale settings (bucket K)

### A warranty needs both `individual` and `corporate` periods (verified 2026-07-18, sandbox)

`POST /after-sales-service-conditions/warranties` and `PUT …/warranties/{id}` reject a request
that omits either buyer-class period with `HTTP 422 UNPROCESSABLE_ENTITY` and a single field
error whose `path` names the missing one — `path=corporate` when only `individual` is set,
`path=individual` when only `corporate` is set (both directions verified live on the seller
sandbox account TestBoxSDK, id 111332841). The spec marks **neither** `required`. A request
that carries both periods succeeds and reads back cleanly (create→get round-trip green). The
SDK's `WarrantyRequest` builder therefore requires both fail-fast, turning the opaque 422 into
a client-side `IllegalStateException` naming the field — no wasted round-trip.

## Web UI anti-bot — DataDome (E2E layer, bucket A / core)

### The buyer web UI escalates to an interactive CAPTCHA from datacenter IPs (verified 2026-07-18, sandbox)

Allegro fronts its **web UI** (login + the OAuth device-flow consent page) with DataDome.
From this container's datacenter IP two distinct challenge tiers were observed:

- **JS interstitial** (self-clearing) — "Potwierdź, że jesteś człowiekiem" served on a 403/429;
  it sets a cookie and clears after a short wait + one reload. The `allegro-e2e` login recipe
  handles exactly this tier (and only this tier).
- **Interactive slider/audio CAPTCHA** (does NOT self-clear) — the same heading, but the
  `geo.captcha-delivery.com` iframe presents a "Przesuń w prawo, aby zabezpieczyć dostęp"
  slider puzzle (plus audio + reload controls). Observed on the **device-flow consent URL**
  (`verification_uri_complete`) when reached with a stale/aged DataDome cookie.

The SDK's REST calls are unaffected — this is purely the browser-facing web UI (login,
device-consent, and the web-only buyer actions). Implications, baked into the code and tests:

- `allegro-e2e` does **not** attempt to solve the interactive puzzle — automating an anti-bot
  CAPTCHA is detection evasion. `BuyerBrowser` detects any DataDome challenge
  (`isDataDomeChallenge`) and fails loudly on the puzzle tier instead of hanging or
  false-passing.
- The **buyer device-flow token** is therefore minted by a one-time human consent click in a
  normal browser (residential IP → no CAPTCHA): run `:allegro-demo:run
  -Pdemo.scenario=auth-bootstrap -Pdemo.account=buyer`, open the printed URL, click *Authorize*.
  Every later run reuses the stored refresh token non-interactively (rotation-safe store).
- Fresh **web-session** automation (buy-now/dispute) from this IP is blocked at the puzzle tier
  the same way; seed the `storageState`/session from a clean IP or by hand when needed.
- DataDome hard-blocks the IP after a burst of rapid logins ("Zostałeś zablokowany… w tej
  samej sieci operuje robot") — hence the `allegro-e2e` serial + rate-limited + login-once
  discipline (`TESTING.md` §3).

## Orders (bucket B)

### `/order/events` pages by an exclusive `from` cursor, not offset/limit (spec-derived, pending live verification)

The order event log (`GET /order/events`) is paginated by a **cursor**, not offset/limit: the
`from` query parameter takes the last event id seen and the response returns the events *after*
it (exclusive), newest walk terminating on an empty page. The SDK's `streamEvents(...)` therefore
uses `PagedSpliterator.cursorStream`, advancing `from` to the last event id of each page and
stopping on the first empty page. Correct termination **depends on `from` being exclusive** — if
the server were inclusive, a non-empty page repeating the last event would not terminate (the
empty-page guard does not catch this). The `from`/`type`/`limit` params are confirmed against the
spec; the exclusivity and the terminal empty-page behaviour are **to be confirmed on the sandbox**
once a seeded order produces events (the RAG digest generically labels this endpoint
"offset/limit", which is inaccurate for `/order/events`).

### Customer-returns are BETA; the reject-refund POST needs a beta request Content-Type (core follow-up)

The customer-returns endpoints (`GET /order/customer-returns`, `GET …/{id}`,
`POST …/{id}/rejection`) speak the **beta** vendor media type. The SDK sends
`Accept: application/vnd.allegro.beta.v1+json` via `HttpCall.acceptBeta()` — correct for the two
GETs. But `POST …/rejection` declares its **request body** only as the beta media type, while
`HttpCall.jsonBody(...)` hard-pins `Content-Type: application/vnd.allegro.public.v1+json` (frozen
runtime), so `returns().rejectRefund(...)` currently sends a mismatched Content-Type that the beta
endpoint may reject (415/406). **Core follow-up (agent-1):** add a media-type overload to the JSON
body helper (e.g. `jsonBody(body, mediaType)`) so beta writes can set the beta Content-Type; it is
the first beta POST-with-body in the SDK. `rejectRefund` ships wired but must not be relied on live
until that lands (WireMock covers the request body shape; the mismatch is header-only).

## Payments & billing (bucket B)

### `GET /billing/billing-types` works with an app-only token and returns a bare array (verified 2026-07-18, sandbox)

`billing().types()` succeeds with a client-credentials (application) token — the endpoint
declares no OAuth scope. The live sandbox returned **234** billing types as a top-level JSON
array (no wrapper object), each `{id, description}`; the SDK deserializes to `BillingType[]` and
maps cleanly. Confirmed via the `billing-types` demo.

### Billing-entries, payment-operations and refunds shapes are spec-derived (pending live verification)

`GET /billing/billing-entries`, `GET /payments/payment-operations`, `GET /payments/refunds` and
`POST /payments/refunds` are wrapped from the spec; their fixtures are `spec-derived` pending the
§2 sandbox pass (blocked on the shared seller token). Notes for that pass: `BillingEntriesRaw`
carries **no** `count`/`totalCount` (the SDK paginates by short-page termination); a billing
entry's `value`/`balance` may be partially populated, so the SDK maps an absent amount/currency
to a `null` `Money` rather than throwing. `POST /payments/refunds` is a **destructive money
movement** — verify it only against a disposable sandbox payment, and reuse the idempotency
`commandId` on any retry.

## Post-sale comms (bucket J)

### Message attachments are not downloadable straight after upload (verified 2026-07-18, sandbox)

The message-attachment flow is `declareAttachment` (POST) → `uploadAttachment` (PUT bytes) →
reference the id in a message. Both the declare and the upload succeed and return the attachment
id, but `GET /messaging/message-attachments/{id}` on that just-uploaded id returns
**`404 NOT_FOUND`** (surfaced as `AllegroNotFoundException`) — the attachment only becomes
retrievable once it has been referenced in a **delivered** message and scanned (its
`MessageAttachment.status()` reaches `SAFE`). Consequence for consumers: do not upload-then-
download the same id as a health check; download an attachment you obtained from a received
message. The SDK surfaces the 404 correctly; the `messaging` demo verifies declare+upload
seller-side and treats the immediate download 404 as expected.

## Offers extras (bucket F)

### Translation PATCH sends `description`/`safetyInformation` as explicit `null` (spec-derived, pending live verification)

`PATCH /sale/offers/{offerId}/translations/{language}` (`offers().translations().update`)
serializes the `ManualTranslationUpdateRequest` through the shared SDK ObjectMapper, which uses
Jackson's default `ALWAYS` inclusion. So a title-only update sends
`{"description":null,"title":{…},"safetyInformation":null}`. It must be verified on the sandbox
whether the server treats those `null` siblings as **"no change"** (safe) or **"clear the
translation"** (a data-loss surprise for a consumer who only meant to set the title). If the
latter, the fix is core-level (NON_NULL serialization inclusion, or per-field `JsonNullable`
handling on writes) — a BACKLOG item for the core owner, affecting every SDK write. Until then,
`update` is safe for offers whose description/safety translations are not manually set.

## Offers — create (bucket A)

### `POST /sale/product-offers` rejects `language: null` — send only set fields (verified 2026-07-18, sandbox)

The generated `SaleProductOfferRequestV1` pre-initializes collection fields to empty and leaves
nullable scalars (notably `language`) null. Serialized with the default mapper, the create body
carries `"language": null`, which the server rejects with a 400
`{path: "language", code: "JsonMappingException", userMessage: "Request contains invalid data"}`.
The SDK therefore serializes create/edit bodies with `HttpCall.jsonBodyPartial` (NON_EMPTY —
omits nulls and empties), so only the fields actually set reach the wire.

### A sellable offer requires seller-account terms + a default shipping list (verified 2026-07-18, sandbox)

With a valid leaf category and a well-formed request, `create` still 400s on a fresh seller
account with three account-configuration errors (not SDK/request faults):
- `afterSalesServices.impliedWarranty` → `ImpliedWarrantyNotDefinedException` ("no Complaints Terms")
- `afterSalesServices.returnPolicy` → `ReturnPolicyNotDefinedException` ("no Returns Terms")
- `delivery.shippingRates` → `DefaultShippingRatesNotFoundException` ("no default shipping price list")

So seeding a live offer needs the seller account to first have warranty terms, return terms and a
default shipping rate list — configured in the sandbox UI, or referenced by id in the create
request (a richer `CreateOfferRequest` that carries `afterSalesServices`/`delivery` refs — future
work). Category ids must be real leaves: `353` (Etui i pokrowce) exists; the placeholder `257`
returns `CATEGORY_NOT_EXISTS`.

## From external sources (to verify on first contact)

- **Sandbox seller accounts may require team-side activation** before the first offer
  listing (`VALIDATION_ERROR` / `SellerAccountVerified`); activation is granted on request in
  the allegro-api GitHub forum. Source: allegro-api issue #5101. To be verified by bucket A's
  starter slice.
- **Sandbox Sales Center UI can lag behind the API** (orders placed but not visible in the
  web UI). Trust the API view. Source: allegro-api issue #9778.
