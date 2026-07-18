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
- `HttpCall.jsonBodyPartial(body)` — serialize a request body omitting null AND
  empty fields, for partial (PATCH) updates where an unset field must be absent
  from the payload rather than sent as `null`/`[]` (which would reset it
  server-side). NON_EMPTY is required because the generated request DTOs
  pre-initialize collection fields to empty. Reusable by every bucket's
  partial-update endpoints.
- Fixed: the shared refresh-token store (`allegro-demo` `SharedTokenStore`) now
  writes crash-atomically (serialize to a sibling temp file, then `ATOMIC_MOVE`)
  under a stable lock file, instead of truncating the data file in place. A demo
  killed mid-write (e.g. at its timeout while refreshing) previously left a
  truncated file that dropped the other account's token; the write is now
  all-or-nothing. First regression tests for this agent infrastructure.
- `HttpCall.betaJsonBody(body)` / `betaJsonBodyPartial(body)` — send a request
  body with the beta vendor content type (`application/vnd.allegro.beta.v1+json`)
  instead of `public.v1`. Beta write surfaces reject the v1 content type, and
  `acceptBeta()` only flips the `Accept` header. Unblocks bucket J's dispute
  writes (post-purchase issues) and bucket B's `returns().rejectRefund`.
- Forward compatibility for enums: generated `*Raw` enums now use
  `enumUnknownDefaultCase`, so `fromValue()` returns an `UNKNOWN_DEFAULT_OPEN_API`
  sentinel for a value Allegro adds later instead of throwing and failing the
  whole response. Domain enums map that sentinel (and any unmodelled value) to
  their own `UNKNOWN` via a switch default; enums that had no `UNKNOWN` gained a
  read-only sentinel constant. Affected domain buckets should add coverage —
  see BACKLOG C3 follow-ups.
- Forward compatibility for discriminated polymorphic responses: a shared
  `UnknownSubtypeToBaseHandler` (Jackson `DeserializationProblemHandler`) resolves
  a `@JsonTypeInfo` subtype discriminator this SDK release does not model to the
  polymorphic base instead of throwing `InvalidTypeIdException`, so a domain mapper
  can degrade it (e.g. `CategoryParameterType.OTHER`) rather than failing the whole
  response. Applies to every discriminated `*Raw` base with a concrete base type.
- Cross-host attachment upload: `HttpCall.fetchLocation(type)` returns the response
  body plus its `Location` header (`Located<T>`), and `HttpCall.putAbsolute(url)`
  PUTs a binary body to an ABSOLUTE URL, bypassing the API base. Allegro declares an
  attachment (`POST .../attachments`) and returns the upload URL on a different host
  (`upload.allegro.pl`) in `Location`; the binary is then PUT there (still Bearer-authed).
  Because that `Location` is plaintext `http`, the token-bearing request FORCES `https`
  for an Allegro upload host (and mirrors the base scheme for the base host), and REFUSES
  to send the access token to any non-Allegro host — the Bearer never travels in the clear
  or to a foreign host. Unblocks offer/after-sales/issue attachment uploads (buckets A, K, J).
- Strict `oneOf` resolution (`StrictOneOfModule`): the generated `oneOf` wrappers
  trial each branch with the lenient mapper (`FAIL_ON_UNKNOWN_PROPERTIES=false`), so
  a payload for one branch also "matches" a sibling by ignoring foreign properties —
  a structural `oneOf` then fails with "N classes match, expected 1". The module
  re-points each wrapper (via a mix-in overriding the generator's `@JsonDeserialize`)
  at a resolver that trials branches with unknown-property strictness, so only the
  branch the payload actually fits wins; it falls back to the lenient single match
  and then an `Object` branch, never worse than before. Buckets that hand-read such
  `oneOf`s from `JsonNode` (pricing, campaigns) can now deserialize them directly.

### A — offers-core

- `client.offers()` starter slice: `get(offerId)` (full product-offer read → immutable
  `Offer` record with `OfferFormat`/`OfferStatus` enums and the shared `Money` Buy Now price)
  and `changeBuyNowPrice(offerId, Money)` (single-offer price-change command). `docs/offers.md`
  + compiled example + `offer` demo scenario (write→read on the sandbox).
- Read/query slice: `streamOffers(OfferFilter)` — a lazy `Stream<OfferSummary>` over the
  seller's offers (offset/limit paging, filter by name/status/format/price) — and
  `smartClassification(offerId)` (Allegro Smart! report → `SmartClassification` with its
  per-condition breakdown). New `OfferFilter` builder; `offer` demo lists offers when no
  `-Pdemo.offerId` is given (live-verified on the sandbox seller account).
- Batch slice: `offers().batch()` sub-facade with `publish(offerIds)` / `unpublish(offerIds)` —
  bulk publish/unpublish wrapped sync-default over `CommandPoller` (submit → poll to terminal →
  gather every task page), returning a `BatchReport` of per-offer `TaskResult`s. No
  `CompletableFuture` in the surface.
- Batch price/quantity: `batch().changePrices(offerIds, Money)` sets a fixed Buy Now price and
  `batch().changeQuantities(offerIds, quantity)` sets available stock across many offers — same
  submit→poll→gather `BatchReport` flow, on the price-change and quantity-change command endpoints.
- `streamUnfilledParameters()` — a lazy `Stream<UnfilledParameters>` over the seller's offers that
  are still missing category parameters (offset/limit paging), each carrying the offer id, its
  category, and the missing parameter ids.
- Write slice: `create(CreateOfferRequest)` — create a Buy Now offer (name, category, price, stock,
  optional images) via a fail-fast builder, returning the created `Offer` (starts as a draft;
  publish with `batch().publish(...)`) — and `deleteDraft(offerId)`.
- `edit(offerId, EditOfferRequest)` — a partial PATCH: only the fields set on the request
  (name / Buy Now price / stock / images) are changed, serialized via the transport's
  `jsonBodyPartial` so untouched fields are absent from the wire rather than reset.
- `offers().promoOptions()` sub-facade (reads): `availablePackages()` — the promotion packages
  a seller can apply (`AvailablePromotionPackages` of `PromotionPackage`s) — and
  `forOffer(offerId)` — the packages currently applied to an offer (`OfferPromoOptions` with
  base/extra `AppliedPromoOption`s and their validity windows).
- Fix: `create` now serializes its body with `jsonBodyPartial` (like `edit`) — the generated
  request type left `language` null and pre-initialized empty collections, which the default
  serializer sent as `"language": null` and Allegro rejected with a 400 (live-caught on the
  sandbox). Only the fields actually set are sent.

### B — orders-payments

- Orders facade (`AllegroClient.orders()`) starter slice: `get(orderId)` fetches a
  single order (checkout form) as an immutable `Order` record — buyer-side
  `OrderStatus`, seller-side `SellerStatus`, buyer, line items, and `Money` totals.
  WireMock contract tests (happy path + 400/401-replay/404/429/5xx), `docs/orders.md`,
  a compiled example, and the `orders-get` sandbox probe.
- Order-management surface: `streamOrders(OrderFilter)` (lazy offset `Stream<Order>`),
  `streamEvents(OrderEventFilter)` (lazy cursor stream) + `eventStats()`,
  `markStatus`/`setSerialNumbers` with optional `revision` optimistic concurrency,
  `attachBillingDocumentLink`, `trackingNumbers`/`addTrackingNumber`, and the
  `carriers()`/`carrierTracking()`/`allegroPickupPoints()` dictionaries. New models
  (`OrderEvent`, `OrderEventStats`, `Waybill`, `Carrier`, `CarrierTracking`,
  `PickupPoint`) and fluent filter/request builders. `orders-list` sandbox probe.
- `client.payments()` facade: `streamOperations(PaymentOperationFilter)` and
  `streamRefunds(RefundFilter)` (lazy offset streams) and `refund(RefundRequest)`
  — initiate a full refund with an idempotency `commandId` and typed `RefundReason`
  (UUID-validated fail-fast). Models `PaymentOperation`/`PaymentRefund`; `docs/payments.md`.
- `client.billing()` facade: `streamEntries(BillingFilter)` (lazy stream) and `types()`
  (billing-type dictionary; public, app-token friendly — **live-verified on the sandbox: 234
  types**). Models `BillingEntry`/`BillingType`; `docs/billing.md` and the `billing-types` probe.
- Orders sub-facades: `orders().invoices()` (`ofOrder`, `declare`, binary `uploadFile`),
  `orders().returns()` (BETA: `streamReturns`, `get`, `rejectRefund` with a typed
  `ReturnRejectionCode`) and `orders().commissionRefunds()` (`streamClaims`, `get`, `claim`,
  `cancel`). New models `OrderInvoice`/`CustomerReturn`/`RefundClaim`/`ReturnRejectionCode` and
  fluent request/filter builders. **All 27 bucket-B operations are now on the SDK surface.**
  Live write→read verification of the order-keyed endpoints remains tracked (needs a seeded
  buyer order; `returns().rejectRefund` also needs the core beta JSON-body Content-Type fix —
  see `KNOWN-SERVER-BEHAVIORS.md`), so their WireMock fixtures stay `spec-derived` meanwhile.

### C — shipping

- `shipping()` facade with the points-of-service sub-facade (starter slice):
  `points().create(PointOfServiceRequest)`, `points().get(id)` and
  `points().delete(id)`, immutable `PointOfService` records with fluent builders
  (fail-fast required fields, replicated length limits) and read-only enum
  fallbacks.
- `points().list()` / `points().list(countryCode)` (returns a
  `List<PointOfService>` — the endpoint is not paginated) and
  `points().update(id, PointOfServiceRequest)`, completing the points-of-service
  CRUD surface. The SDK resolves the seller id from the token, so no `sellerId`
  argument is needed.
- `deliveryMethods()` — lists the seller's available delivery methods
  (`List<DeliveryMethod>`, with `PaymentPolicy`); read-only, works with an
  application token (no user scope required).
- Live sandbox verification of the points-of-service write path surfaced three
  spec-vs-wire divergences, now handled by the SDK: create/update require
  `seller.id` in the body (resolved from the token) and `coordinates` on the
  address (now a required builder field), and the update `PUT` requires the `id`
  in the body. create → get → update → delete verified green on the sandbox.

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
- `bidding` demo scenario (`allegro-demo`, not published) — the buyer half of
  bucket D's live verification, run with a stored buyer token
  (`-Pdemo.scenario=bidding -Pdemo.account=buyer`). Always probes the read path
  (`myBid` on a non-existent auction → `AllegroNotFoundException`); `myBid`/
  `placeBid`→`myBid` write→read on a real auction stay gated behind
  `-Pdemo.offerId`/`-Pdemo.bidAmount`. Verified live 2026-07-18: a buyer token
  (account distinct from the seller) reaches the bidding API and maps its 404s.

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
- `client.catalog().products().search(ProductSearchRequest)` — lazily search the
  product database (`GET /sale/products`), returning a `Stream<ProductSummary>`
  (id, name, category, publication status, image URLs) that follows Allegro's opaque
  `page.id` cursor automatically. The fail-fast `ProductSearchRequest` builder
  requires a phrase (category is an optional phrase-scoped filter). Live
  `catalog-products` demo scenario.
- `catalog().products().get(productId)` — read a product
  (`GET /sale/products/{id}`) as an immutable `Product`: id, name, category,
  publication status, protected-brand flag, image URLs, and the
  `ProductParameterValue` list (localized `values` + stable `valuesIds`)
  describing it. A focused projection — the structured `description` and
  compatibility blocks follow in a later slice. The `catalog-products` demo reads
  a searched product back (search → get round-trip).

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
- `classifieds.offerStats(offerIds, ClassifiedStatsFilter)` and
  `classifieds.sellerStats(ClassifiedStatsFilter)` — daily advertisement
  statistics for up to 50 selected offers, or aggregated across the seller
  (`/sale/classified-offers-stats`, `/sale/classified-seller-stats`). Returns
  per-event totals (`ClassifiedEventType`) and a day-by-day breakdown
  (`ClassifiedDailyStat`); `ClassifiedStatsFilter` carries the optional
  `date.gte`/`date.lte` bounds. Completes the standalone `classifieds()` surface.
- `offers().tags()` — the first offers-attached sub-facade: the seller's private
  offer tags (`streamTags()` lazy, `create`/`rename`/`delete`) and their
  per-offer assignment (`ofOffer`, `assignToOffer`). Immutable `Tag` records +
  fail-fast `TagRequest` builder; `offer-tags` write→read demo. New `offerextras`
  package wired onto the bucket-A `Offers` root.
- `offers().translations()` — an offer's translations into other languages:
  `ofOffer` (read), `update` (set the title translation), `delete`. Immutable
  `OfferTranslation` records (`title` + `titleType`) + `TranslationRequest`
  builder. Title translation only for now; description/safety-information
  translations (rich structured content) are a documented follow-up.
- `offers().rating(offerId)` — an offer's aggregated buyer rating (`OfferRating`:
  average, total, score distribution, size feedback). Read-only.
- `offers().bundles()` — the seller's fixed offer bundles: `streamBundles()`
  (lazy cursor stream), `get`, `updateDiscount(bundleId, List<BundleDiscount>)`
  (PUT the per-marketplace discounts, returns the updated bundle), `delete`.
  Immutable `OfferBundle` (bundled `offers`, per-marketplace `discounts` as
  `Money`, `publications`, `createdBy`) with `BundledOffer`/`BundleDiscount`/
  `BundlePublication` records.
- `offers().flexibleBundles()` — the seller's flexible bundles: `streamBundles()`
  (lazy cursor stream of `FlexibleBundleSummary`), `get` (full `FlexibleBundle`
  with slots/offers and the discriminated whole-bundle or per-slot discount),
  `delete`. Records `FlexibleBundle`/`FlexibleBundleSummary`/`FlexibleBundleSlot`/
  `FlexibleBundleSlotOffer`/`FlexibleBundleDiscount`/`WholeBundleDiscount`/
  `SlotDiscount`/`MarketplaceDiscount`. Create/update (the nested slot/offer/
  discount write builders) are a planned follow-up. **Completes the bucket-F
  offers-extras surface (classifieds + tags + translations + rating + bundles +
  flexible bundles); flexible-bundle writes are the one deferred item.**

### G — pricing
- Automatic pricing rules starter slice: `client.pricing().automation()` with
  `create` / `get` / `delete`, the immutable `PricingRule` record (sealed
  `PricingRuleConfiguration` for amount/percentage adjustments, shared `Money`),
  and the fail-fast `PricingRuleRequest` fluent builder. WireMock-covered
  (request shape, oneOf mapping, full error-path table) with the `pricing` demo
  performing a live sandbox write→read→teardown.
- Automatic pricing completed: `automation().rules()` (list incl. built-in
  defaults), `automation().update(ruleId, PricingRuleEdit)` (edit name +
  configuration; the immutable type is not part of an edit), and
  `automation().rulesOfOffer(offerId)` returning the per-marketplace
  `OfferPricingRules` assignments with their price bands.
- `pricing().feePreview(OfferFeePreviewRequest)` — preview an offer's sale
  commission and recurring quotes for a category + Buy Now price, mapped to
  `FeePreview` (`FeeCommission` / `FeeQuote`).
- `pricing().quotes(List<String> offerIds)` — the seller's current fee quotes
  (repeated `offer.id`, `billing:read`), mapped to `OfferQuote`.
- `pricing().turnoverDiscounts()` — `list()` / `list(marketplaceId)` /
  `set(marketplaceId, TurnoverDiscountRequest)` / `deactivate(marketplaceId)`,
  with the immutable `TurnoverDiscount` (dated `TurnoverDiscountDefinition`s and
  `TurnoverThreshold` ladders) and its fail-fast request builder.
- `pricing().depositTypes()` — the deposit types available for offers, mapped to
  `DepositType`.
- `pricing().promotions()` — rebate promotions: `streamPromotions(PromotionType)`
  / `streamPromotions(PromotionType, offerId)` (lazy offset/limit `Stream`),
  `get` / `create` / `modify` / `deactivate`. The polymorphic `benefits[]` map to
  a sealed `Benefit` hierarchy (`LargeOrderDiscount` / `MultiPackDiscount` /
  `WholesalePriceList`), with `OfferCriterion` targeting and the fail-fast
  `PromotionRequest` builder. Benefit families deserialize natively; an unknown
  family degrades to `Benefit.UnknownBenefit` (unknown-subtype forward-compat
  core) and an unmodelled promotion status or offer-criterion type degrades to
  its `UNKNOWN` enum constant instead of failing the read, completing bucket G at
  17/17 ops.

### H — campaigns

- `client.campaigns().badges().availableCampaigns()` — list badge campaigns available to the
  authenticated seller (GET `/sale/badge-campaigns`), with an optional per-marketplace overload.
  Immutable `BadgeCampaign` model with eligibility, refusal reasons and the application/visibility/
  publication schedules. Starter slice of bucket H.
- Complete the badges sub-facade: `apply(BadgeApplicationRequest)` (POST `/sale/badges`, returned
  without blocking — badge verification is e-mail-notified and asynchronous), `streamApplications`
  (GET `/sale/badge-applications`) and `application(id)`, `streamBadges` (GET `/sale/badges`), and
  `update(offerId, campaignId, BadgePatch[, Duration])` (PATCH, polled to a terminal badge operation
  via the shared command poller). Adds `BadgeApplication`, `Badge`, `BadgePrices`, `BadgeOperation`
  models and their status/type enums, the `BadgeApplicationRequest`/`BadgeApplicationFilter`/
  `BadgeFilter` builders and the `BadgePatch` change type.
- Add the Allegro Prices sub-facade `client.campaigns().allegroPrices()`: `participation` /
  `updateParticipation(ParticipationUpdate)` (GET/PATCH `/sale/allegro-prices/accounts/participations`),
  `streamOffersStatus(AllegroPricesOfferQuery)` (POST `/sale/allegro-prices/offers-queries`, lazy
  stream mapped from raw JSON to side-step the generated `oneOf` price-reduction deserializer), and
  the subsidy commands `submitOffers`/`excludeOffers(...[, Duration])` (POST, polled to a terminal
  per-offer report). Adds `AllegroPricesParticipation`/`MarketplaceParticipation`/`ParticipationStatus`,
  `AllegroPricesOfferStatus`, `SubsidyCommandReport`/`SubsidyOfferResult`/`SubsidyOfferStatus` models
  and the `ParticipationUpdate`/`AllegroPricesOfferQuery`/`SubmitOffersRequest`/`ExcludeOffersRequest`
  builders (with `OfferScope`/`OfferSubstatus` filters).
- Add the AlleDiscount sub-facade `client.campaigns().alleDiscount()` — completes bucket H:
  `campaigns()` (GET `/sale/alle-discount/campaigns`, a `List`), lazy `streamEligibleOffers`/
  `streamSubmittedOffers` (GET `…/{campaignId}/eligible-offers` and `…/submitted-offers`), and the
  `submitOffer`/`withdrawOffer(...[, Duration])` commands (POST, polled to a terminal result). Adds
  `AlleDiscountCampaign`, `AlleDiscountEligibleOffer`, `AlleDiscountSubmittedOffer`,
  `AlleDiscountSubmitResult`/`AlleDiscountWithdrawResult` and their status/type enums (plus the shared
  `ConditionViolation`), and the `SubmitOfferRequest`/`EligibleOffersFilter`/`SubmittedOffersFilter`
  builders.

### I — fulfillment

- Add `client.fulfillment()` (One Fulfillment by Allegro) with the removal-preference starter
  slice: `removalPreference()` (read) and `setRemovalPreference(...)` (write), the
  `RemovalPreference` / `WithdrawalAddress` / `PhoneNumber` records, the `RemovalOperation`
  enum, and their fluent builders. Consumer guide: `docs/fulfillment.md`.
- Add the fulfillment read reports and tax-id resource: lazy `stock()` / `stock(StockFilter)`,
  `availableProducts()` and `refundDispositions()` / `refundDispositions(RefundDispositionFilter)`
  streams, `parcelsOf(orderId)`, and `taxId()` / `addTaxId(...)` / `updateTaxId(...)`. New immutable
  records (`StockItem` tree, `AvailableProduct`, `FulfillmentOrder`, `RefundDisposition` tree,
  `TaxId`), the `StockFilter` / `RefundDispositionFilter` builders, and forward-compatible
  open-set enums (`ReserveStatus`, `StorageFeeStatus`, `RefundDispositionType`,
  `RefundStockStatus`, `AccountableParty`, `RefundActionState`) that resolve unknown wire
  values to `UNKNOWN`.

### J — post-sale-comms

- Contacts facade (`client.contacts()`) — starter slice: list, get, create and update
  seller contact cards (`/sale/offer-contacts`) with a fluent, fail-fast `ContactRequest`
  builder, immutable `Contact` records, and a `contacts` sandbox write→read demo scenario.
- Messaging facade (`client.messaging()`) — the message center (`/messaging`, 11 ops):
  lazy `streamThreads()`/`streamMessages()` pagination, `thread`/`message` reads, `markRead`,
  `send`/`reply`, `deleteMessage`, and the binary attachment `declare`→`upload`→`download`
  flow. Immutable records (`MessageThread`, `Message`, `MessageAttachment`, `AttachmentRef`)
  with `UNKNOWN`-tolerant enums, fail-fast `NewMessageRequest`/`ReplyRequest`/
  `AttachmentDeclaration`/`MessageFilter` builders, and a `messaging` demo scenario
  (self-seeded attachment round-trip + threads read / `markRead` write→read).
- Disputes facade (`client.disputes()`) — read side of post-purchase issues (`/sale/issues`,
  **beta** media type): lazy `streamIssues(IssueFilter)` (status + checkout-form filter),
  `get(issueId)`, and lazy `streamChat(issueId)`. Immutable `Issue`/`IssueChatEntry` records
  with `UNKNOWN`-tolerant enums (`IssueType`, `IssueRight`, `IssueStatus`, `ChatAuthorRole`),
  a fluent `IssueFilter`, and a `disputes` read-shape demo. The seller-side write operations
  (add message, change status, attach) follow once the shared transport exposes a beta
  request-body media type (backlog item).

### K — sale-settings

- `settings().afterSale()` starter slice: seller **warranty** definitions —
  `streamWarranties()` (lazy offset/limit `Stream`), `warranty(id)`,
  `createWarranty(...)`, `updateWarranty(...)`. Immutable `Warranty` /
  `WarrantySummary` records, `WarrantyType` / `WarrantyPeriod` value types, and a
  fail-fast `WarrantyRequest` builder — both buyer-class periods (`individual` /
  `corporate`) are required, an undocumented server rule (spec marks neither) verified
  live and recorded in `KNOWN-SERVER-BEHAVIORS.md`. Documented in `docs/settings.md`; the
  `settings-warranty` write→read demo is green on the sandbox (create→get round-trip).
