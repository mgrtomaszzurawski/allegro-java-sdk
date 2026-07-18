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

### `deliveryMethods().paymentPolicy` — read-only, fail-soft (verified 2026-07-18, sandbox)

Each method's `paymentPolicy` is one of a known set (`IN_ADVANCE`,
`CASH_ON_DELIVERY`). The generated Layer-1 model is a typed enumeration; since the
core `enumUnknownDefaultCase` change (C3) an unrecognised wire value deserialises
to the generator's `UNKNOWN_DEFAULT_OPEN_API` sentinel instead of throwing. The
SDK's domain `PaymentPolicy` mirrors the bucket's other read enums: `fromWire`
maps that sentinel (and any value this release does not model) to
`PaymentPolicy.UNKNOWN`, so a payment policy Allegro adds in future degrades one
field rather than failing the whole `deliveryMethods()` read. `null` stays `null`
(the server omitted the field). A `ShippingEnumsTest` parity test still iterates
both enums and fails the build if a spec regeneration renames a real value on one
side only (which would silently degrade a known policy to `UNKNOWN`).

### Creating a point of service requires `seller.id` and `coordinates` (verified 2026-07-18, sandbox)

`POST /points-of-service` (and `PUT /points-of-service/{id}`) reject a
spec-conformant body with `HttpMessageNotReadableException` / "Invalid data
format" (a body-level parse error, `path=null`) unless it also carries
`seller.id`, even though the vendored spec does **not** mark `seller` required.
Once `seller.id` is present the parse succeeds and a second, stricter constraint
surfaces: `address.coordinates` is required too (spec marks it optional) —
`ConstraintViolationException` on `path=address.coordinates`. The `PUT` body
additionally requires the `id` (the spec notes this: "required when updating").

The SDK handles all three: it resolves the seller id from the token (`GET /me`,
cached) and injects `seller.id` into the create/update body and the list query;
`AddressBuilder` makes `coordinates` a required (fail-fast) field; and the update
client sets the body `id` from the path id. Confirmed live end-to-end:
create → get → update → delete round-trips green. Opening-hours `from`/`to` use
the ISO `HH:mm:ss.SSS` time format (spec example `10:30:00.000`);
`confirmationType` `AWAIT_CONTACT` is accepted.

### Delivery settings read + write verified; free-delivery thresholds may be null (verified 2026-07-18, sandbox)

`GET /sale/delivery-settings` maps cleanly (`marketplace.id`, `joinPolicy.strategy`,
`updatedAt`) and an **idempotent** `PUT` (re-sending the read state) round-trips
green — unlike the point-of-service write path, delivery settings need no
`seller.id` or other undocumented field. On the sandbox seller the
`freeDelivery` / `abroadFreeDelivery` objects are **absent** (the seller offers no
free-delivery threshold), so the SDK maps them to a `null` `Money`; `joinPolicy`
came back `MAX`. The PUT `Content-Type` is the vendor `v1` media type (not beta).

### Shipping-rate sets: read verified deep; every sandbox set is Allegro-managed (verified 2026-07-18, sandbox)

`GET /sale/shipping-rates` returned **7** sets and `GET /sale/shipping-rates/{id}`
mapped a 47-row set in full (each row's `deliveryMethod.id`, `firstItemRate` /
`nextItemRate` as `Money` — a `0.00` next-item rate is normal, `maxQuantityPerPackage`,
and the optional `maxPackageWeight` / `shippingTime` arriving `null`). **All 7 sets
report `features.managedByAllegro = true`** (One Fulfillment sets), so the write
path could not be exercised idempotently on this account, and the delivery API
subset has **no delete** operation — a probe-created set would linger permanently.
The rates **write** path (create/PUT) is therefore pinned by the WireMock contract
tests (body serialization incl. nested rows, path, headers, and the id echoed in
the PUT body) rather than live-verified; it shares the exact transport plumbing
that the delivery-settings PUT proved live. Re-verify a live write if a
seller-editable (non-managed) set is created on the sandbox account.

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

### Smart! condition `value`/`threshold` is boolean OR number on the wire (verified 2026-07-18, sandbox)

`GET /sale/smart` returns each entry in `conditions[]` with a `value` and `threshold` that are a
JSON **number** for a metric condition (e.g. `1.5` days) but a JSON **boolean** for a pass/fail
condition (e.g. `false`) — the spec types both as `number`, so the generated Layer-1 DTO
(`SmartSellerClassificationReportConditionsInnerRaw`) fields are `BigDecimal` and Jackson aborts
the WHOLE response with `MismatchedInputException` the moment a boolean value appears. Live-caught
running the `account` seller demo (`me()` and `salesQuality()` succeed; `smartClassification()`
threw). The SDK now reads the response from a `JsonNode` (`SmartClassificationMapper`), keeping a
numeric value/threshold typed as `BigDecimal` and mapping a boolean one to `null` — the pass/fail
outcome is carried by the condition's `fulfilled` flag. Not a `oneOf`/enum/subtype case, so the
core C3–C5 forward-compat handlers do not apply.

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

### Implied-warranty periods are whole years, at least two (verified 2026-07-18, sandbox)

`POST /after-sales-service-conditions/implied-warranties` (and the `PUT`) accept a period only as
a **whole-year** ISO-8601 duration of **at least two years** (`P2Y`). Verified live on seller
TestBoxSDK: `P2Y` on both `individual` and `corporate` succeeds and reads back cleanly
(create→get round-trip green); `P1Y` is rejected with `422 UNPROCESSABLE_ENTITY path=corporate.period`
(minimum is two years, the statutory rękojmia term); the month-denominated forms `P12M` and `P24M`
are rejected on both `individual.period` and `corporate.period`. This differs from the seller
`warranties` endpoint, whose periods are month-denominated (`P12M`) and may be lifetime. The SDK
leaves the exact value to the server (it can change per legal category) and documents the rule on
`ImpliedWarrantyPeriod`; only the structural `name`/`individual` requirements are builder-enforced.

### A return policy with returns enabled requires `options` (verified 2026-07-18, sandbox)

`POST /after-sales-service-conditions/return-policies` (and the `PUT`) reject a body whose
`availability.range` is not `DISABLED` but which omits the `options` object, with
`422 UNPROCESSABLE_ENTITY code=AVAILABILITY_ENABLED_RETURN_OPTIONS_INVALID` (`path=null`, a
body-level error). The spec marks `ReturnPolicyOptions` `nullable: true` with "Can be null if
availability range is 'DISABLED'", and all five booleans are `required` within it. Verified live
on seller TestBoxSDK: a `FULL`/`RESTRICTED` policy with `options` (all five flags) creates, reads
back, updates and deletes cleanly (full round-trip green); the same request without `options` is
rejected. The SDK's `ReturnPolicyRequest`/`ReturnPolicyUpdateRequest` builders therefore require
`options` fail-fast whenever the range is not `DISABLED`, turning the opaque 422 into a
client-side `IllegalStateException`.

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

### Customer-returns are BETA on both sides; the reject-refund POST body uses the beta Content-Type (RESOLVED)

The customer-returns endpoints (`GET /order/customer-returns`, `GET …/{id}`,
`POST …/{id}/rejection`) speak the **beta** vendor media type. The SDK sends
`Accept: application/vnd.allegro.beta.v1+json` via `HttpCall.acceptBeta()` on all three, and
`POST …/rejection` declares its **request body** only as the beta media type. Earlier this was a
header mismatch (the frozen `HttpCall.jsonBody(...)` hard-pinned `Content-Type:
application/vnd.allegro.public.v1+json`). **Resolved 2026-07-18:** the core added
`HttpCall.betaJsonBody(...)` (PR #63, `2958e1f`), and `returns().rejectRefund(...)` now sends the
request body with `Content-Type: application/vnd.allegro.beta.v1+json` (asserted on the wire in
`CustomerReturnsClientTest`). No remaining Content-Type mismatch; live write→read still pending a
seeded buyer return.

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

### Translation PATCH must omit unset parts, not send them as `null` — FIXED (partial body; spec-derived)

`PATCH /sale/offers/{offerId}/translations/{language}` (`offers().translations().update`) is a
partial update: the caller may set any subset of title / description / safety-information, and the
unset parts must be **left untouched**. The earlier implementation serialized through the shared
SDK ObjectMapper's default `ALWAYS` inclusion, so a title-only update sent
`{"description":null,"title":{…},"safetyInformation":null}` — risking that the server reads a
`null` sibling as **"clear the translation"** (latent data loss) rather than "no change". Fixed by
serializing the body with `HttpCall.jsonBodyPartial` (NON_EMPTY — omits nulls and empties, the
same approach bucket A adopted for `POST /sale/product-offers`, §below) and building the raw with
only the parts the request set. A title-only update now sends `{"title":{…}}`; the ambiguity is
gone regardless of server semantics. **Still to confirm on the sandbox** (needs a live offer): that
the server does treat an omitted part as "no change" (expected) and accepts a subset PATCH.

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

## Fulfillment (bucket I)

### `/fulfillment/*` returns 403 for a seller not enrolled in One Fulfillment (verified 2026-07-18, sandbox)

One Fulfillment by Allegro is an invitation-only logistics contract. The shared sandbox seller
account (`TestBoxSDK`, id `111332841`) is **not enrolled**, so every `/fulfillment/*` call
returns **HTTP 403**. Verified live through the SDK (`-Pdemo.scenario=fulfillment
-Pdemo.account=seller`): `fulfillment().removalPreference()` — the first call the probe makes —
threw a typed `AllegroException` with `statusCode() == 403`, which the demo caught and reported
("not enrolled ... verified the typed-exception path"). Consequences for bucket I:

- The happy-path write→read DoD is **not live-runnable** on this sandbox; the SDK contract is
  proven by the WireMock suite instead, and the live layer verifies the **negative path** (the
  403 typed-exception surfaces correctly).
- Response fixtures for the fulfillment reports / ASN lifecycle stay marked
  `// spec-derived: not yet wire-verified` — they cannot be reconciled against real 1F data
  until an enrolled sandbox account exists. If Allegro enrols the sandbox seller later, re-run
  the demo and drop the `spec-derived` marks.

### Advance Ship Notice writes need `If-Match`; submit is a client-keyed async command (spec-derived, pending live verification)

Both `PUT /fulfillment/advance-ship-notices/{id}` (update) and `PUT .../{id}/submitted`
(updateSubmitted) declare the `If-Match` header **required** and echo an `etag` on every
single-notice read/write — optimistic concurrency, not last-write-wins. The SDK surfaces that
`ETag` as `AdvanceShipNotice.version()` and threads it back as the `If-Match` token, so
`update`/`updateSubmitted` take the version explicitly rather than silently re-fetching it.

Submitting a notice is an async command the **client** keys: `PUT
/fulfillment/submit-commands/{command-id}` with a client-generated UUID and body
`{input:{advanceShipNoticeId}}`, then poll `GET .../submit-commands/{command-id}` until
`output.status` leaves `RUNNING` (terminal `SUCCESSFUL` / `FAILED`; the SDK treats any
non-`RUNNING` value as terminal so a future status cannot hang the poll). The SDK generates the
id, polls to a terminal `SubmitStatus`, and returns it (a `FAILED` command is terminal data, not
an exception). The notice's `shipping` is polymorphic (`COURIER_BY_SELLER` / `OWN_TRANSPORT` /
`THIRD_PARTY_DELIVERY`, each with its own required sub-fields) and is a deferred follow-up. None
of this is live-verifiable on the shared sandbox seller (403, not enrolled in One Fulfillment).

## Campaigns (bucket H)

### Allegro Prices offer-status query requires `offer.ids` 1..1000 (verified 2026-07-18, sandbox)

`POST /sale/allegro-prices/offers-queries` (`allegroPrices().streamOffersStatus(...)`) rejects a
marketplace-only query with `400 VALIDATION_ERROR`, `message: "Validation error: offer.ids size
must be between 1 and 1,000"` (example `trace-id` `2b6432bc191218e0`) — even though the vendored
spec types `offer` and `offer.ids` as OPTIONAL. In practice at least one offer id is mandatory.
The SDK stays spec-faithful (the builder does not force `offerId`, so it can serialise a
marketplace-only query) and surfaces the rejection as `AllegroBadRequestException` with the parsed
`errors[]`; consumers must supply `AllegroPricesOfferQuery.builder(marketplace).addOfferId(...)`.
The `campaigns` demo now sources a few of the seller's own offer ids before querying, and skips the
probe when the account has no offers.

### Allegro Prices participation spans four marketplaces (verified 2026-07-18, sandbox)

`GET /sale/allegro-prices/accounts/participations` (`allegroPrices().participation()`) returned four
marketplaces for the sandbox seller — `allegro-pl`, `allegro-cz`, `allegro-sk`, `allegro-hu`, all
`ALLOWED` — confirming the `MarketplaceParticipation` mapping across markets on a live response.

### AlleDiscount exposes SOURCING and DISCOUNT campaign types (verified 2026-07-18, sandbox)

`GET /sale/alle-discount/campaigns` (`alleDiscount().campaigns()`) returned 12 campaigns for the
sandbox seller, spanning both `SOURCING` (co-finance) and `DISCOUNT` (AlleObniżka) campaign types,
confirming the `AlleDiscountCampaign` / `AlleDiscountCampaignType` mapping. Every listed campaign
showed the seller as not enrolled, so `streamEligibleOffers`/`streamSubmittedOffers` returned empty
without error — the write→read command cycles still need an enrolled campaign (risk R5).

## From external sources (to verify on first contact)

- **Sandbox seller accounts may require team-side activation** before the first offer
  listing (`VALIDATION_ERROR` / `SellerAccountVerified`); activation is granted on request in
  the allegro-api GitHub forum. Source: allegro-api issue #5101. To be verified by bucket A's
  starter slice.
- **Sandbox Sales Center UI can lag behind the API** (orders placed but not visible in the
  web UI). Trust the API view. Source: allegro-api issue #9778.
