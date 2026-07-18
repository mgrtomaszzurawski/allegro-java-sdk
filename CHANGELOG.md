# Changelog

All notable changes to this project will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)
(pre-1.0: breaking changes may occur between 0.x releases).

Every merged PR appends its entries ONLY inside its own pre-created bucket subsection
under `[Unreleased]` below — never in another bucket's subsection, never reordering
sections. Empty subsections are dropped by the release engineer when folding
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
- Observability (AWS-SDK model): named SLF4J debug channels
  (`request`/`auth`/`retry`), `traceId()` on every exception (wire-verified
  `trace-id` header), `AllegroExecutionInterceptor` SPI with per-attempt
  callbacks.
- JaCoCo coverage gate active in `check` (85%/84% at activation); 39-test
  WireMock suite per TESTING.md.
- `allegro-demo` probe runner (`auth-bootstrap`, `me`) + flock-guarded shared
  token store (ADR-008); live sandbox verification caught and fixed a
  JsonNullable deserialization defect on `/me`.
- Cross-bucket transport capabilities via the fluent `HttpCall` builder: encoded
  query parameters (`Query`), PATCH, no-content/void writes, beta vendor media
  type, `Accept-Language`, conditional `If-Match`, and binary request bodies.
- Shared value type `sdk.core.Money` (exact-string amount + ISO-4217 currency).
- `allegro-demo` scenario-registration seam (`DemoScenario` + append point) so
  every bucket adds probes without colliding on a shared dispatch.
- `CommandPoller` — the sync-default engine for Allegro's asynchronous command
  endpoints (ADR-005): submit-then-poll to a terminal state with bounded
  backoff, an optional `Duration` timeout, and `AllegroAsyncTimeoutException`;
  status shape and terminal predicate supplied by the caller. Shared by
  buckets A (batch), C (WZA), H (badges/subsidies), I (ASN).
- Binary download (`HttpCall.fetchBytes`) and conditional-read
  (`HttpCall.fetchWithETag` → `Etagged<T>`): the transport response path is now
  generic over the body type, so a byte download still shares retry, 401 replay
  and typed error mapping (the error body is decoded from bytes). Unblocks
  bucket I (PDF, `If-Match`) and bucket J (attachment downloads).

### A — offers-core

- `client.offers()` starter slice: `get(offerId)` (full product-offer read → immutable
  `Offer` record with `OfferFormat`/`OfferStatus` enums and the shared `Money` Buy Now price)
  and `changeBuyNowPrice(offerId, Money)` (single-offer price-change command). `docs/offers.md`
  + compiled example + `offer` demo scenario (write→read on the sandbox).
- Read/query slice: `streamOffers(OfferFilter)` — a lazy `Stream<OfferSummary>` over the
  seller's offers (offset/limit paging, filter by name/status/format/price/sort) — and
  `smartClassification(offerId)` (Allegro Smart! report → `SmartClassification` with its
  per-condition breakdown). New `OfferFilter` builder; `offer` demo lists offers when no
  `-Pdemo.offerId` is given (live-verified on the sandbox seller account).

### B — orders-payments

- Orders facade (`AllegroClient.orders()`) starter slice: `get(orderId)` fetches a
  single order (checkout form) as an immutable `Order` record — buyer-side
  `OrderStatus`, seller-side `SellerStatus`, buyer, line items, and `Money` totals.
  WireMock contract tests (happy path + 400/401-replay/404/429/5xx), `docs/orders.md`,
  a compiled example, and the `orders-get` sandbox probe.

### C — shipping

- `shipping()` facade with the points-of-service sub-facade (starter slice):
  `points().create(PointOfServiceRequest)`, `points().get(id)` and
  `points().delete(id)`, immutable `PointOfService` records with fluent builders
  (fail-fast required fields, replicated length limits) and read-only enum
  fallbacks.
- `points().list(sellerId)` / `points().list(sellerId, countryCode)` (returns a
  `List<PointOfService>` — the endpoint is not paginated) and
  `points().update(id, PointOfServiceRequest)`, completing the points-of-service
  CRUD surface.
- `deliveryMethods()` — lists the seller's available delivery methods
  (`List<DeliveryMethod>`, with `PaymentPolicy`); read-only, works with an
  application token (no user scope required).

### D — account-meta

- `client.marketplaces().list()` — the platform's marketplaces with their
  languages, currencies and shipping countries (`GET /marketplaces`; public,
  works with an app-only token). Bucket D starter slice.
- `client.user()` reports: `salesQuality()` (`GET /sale/quality`) and
  `smartClassification()[(marketplaceId)]` (`GET /sale/smart`).
- `client.user().ratings()`: lazy `stream(RatingFilter)` (`GET /sale/user-ratings`,
  short-page termination), `get`, `answer` (`PUT …/answer`), `requestRemoval`
  (`PUT …/removal`) and `summaryOf(userId)` (`GET /users/{userId}/ratings-summary`).
- `client.user().additionalEmails()`: `list`/`get`/`add`/`delete`
  (`/account/additional-emails`).
- `client.bidding()`: `myBid` and `placeBid` (`/bidding/offers/{offerId}/bid`),
  using the shared `sdk.core.Money`.
- `client.charity().searchCampaigns(CharitySearch)`
  (`GET /charity/fundraising-campaigns`, beta).
- `client.affiliate().streamCpsConversions(ConversionFilter)`
  (`GET /affiliate/conversions/cps`, beta, lazy stream).
- New `sdk.domain.account.builder` package: `RatingAnswer`, `RatingRemoval`,
  `RatingFilter`, `CharitySearch`, `ConversionFilter`.

### E — catalog-products

- Starter slice: `client.catalog().categories()` — the category tree
  (`roots()`, `childrenOf(parentId)`, `get(categoryId)`) with the immutable
  `Category` / `CategoryOptions` records. Read-only, works with an app-only
  client-credentials token. Includes the `catalog-categories` sandbox
  shape-verification demo scenario. Products and compatibility follow in the
  same bucket.
- `categories().parameters(categoryId)` — the parameters a category expects on
  its offers and products, as immutable `CategoryParameter` records: the
  `CategoryParameterType` (dictionary/float/integer/string), a flattened
  `ParameterRestrictions` (numeric bounds/precision, text lengths,
  `multipleChoices`), the dictionary `DictionaryValue`s and
  `CategoryParameterOptions`. An unmodelled parameter type maps to the mapper's
  `CategoryParameterType.OTHER` default (today an unknown `type` still fails
  deserialization on the wire — end-to-end degradation is a tracked core follow-up).
- `categories().suggest(productName)` — categories whose names best match a
  product or offer name (`GET /sale/matching-categories`) as `CategorySuggestion`
  records, each reachable up its parent breadcrumb.

### F — offers-extras

- `client.classifieds()` accessor with `availablePackages(categoryId)` — read the
  advertisement package configurations available in a category
  (`GET /sale/classifieds-packages`). Starter slice of bucket F; immutable
  `ClassifiedPackage` records, WireMock error-path coverage, and a live
  read-shape demo scenario (`-Pdemo.scenario=classifieds`).
- `classifieds.getPackage(packageId)`, `classifieds.packagesOfOffer(offerId)` and
  `classifieds.assignPackages(offerId, ClassifiedAssignment)` — read one package
  configuration, read the packages assigned to an offer, and assign a base plus
  optional extra packages to an offer (`/sale/classifieds-packages/{packageId}`,
  `/sale/offer-classifieds-packages/{offerId}`). `ClassifiedAssignment` is built
  by a fail-fast builder; the write→read demo assigns then reads the packages
  back. All calls require the seller user token.

### G — pricing
- Automatic pricing rules starter slice: `client.pricing().automation()` with
  `create` / `get` / `delete`, the immutable `PricingRule` record (sealed
  `PricingRuleConfiguration` for amount/percentage adjustments, shared `Money`),
  and the fail-fast `PricingRuleRequest` fluent builder. WireMock-covered
  (request shape, oneOf mapping, full error-path table) with the `pricing` demo
  performing a live sandbox write→read→teardown.

### H — campaigns

- `client.campaigns().badges().availableCampaigns()` — list badge campaigns available to the
  authenticated seller (GET `/sale/badge-campaigns`), with an optional per-marketplace overload.
  Immutable `BadgeCampaign` model with eligibility, refusal reasons and the application/visibility/
  publication schedules. Starter slice of bucket H.

### I — fulfillment

- Add `client.fulfillment()` (One Fulfillment by Allegro) with the removal-preference starter
  slice: `removalPreference()` (read) and `setRemovalPreference(...)` (write), the
  `RemovalPreference` / `WithdrawalAddress` / `PhoneNumber` records, the `RemovalOperation`
  enum, and their fluent builders. Consumer guide: `docs/fulfillment.md`.

### J — post-sale-comms

- Contacts facade (`client.contacts()`) — starter slice: list, get, create and update
  seller contact cards (`/sale/offer-contacts`) with a fluent, fail-fast `ContactRequest`
  builder, immutable `Contact` records, and a `contacts` sandbox write→read demo scenario.

### K — sale-settings

- `settings().afterSale()` starter slice: seller **warranty** definitions —
  `streamWarranties()` (lazy offset/limit `Stream`), `warranty(id)`,
  `createWarranty(...)`, `updateWarranty(...)`. Immutable `Warranty` /
  `WarrantySummary` records, `WarrantyType` / `WarrantyPeriod` value types, and a
  fail-fast `WarrantyRequest` builder — both buyer-class periods (`individual` /
  `corporate`) are required, an undocumented server rule (spec marks neither) verified
  live and recorded in `KNOWN-SERVER-BEHAVIORS.md`. Documented in `docs/settings.md`; the
  `settings-warranty` write→read demo is green on the sandbox (create→get round-trip).
