# Offers

The offer lifecycle, reached from `client.offers()`.

> Available now: reading/listing offers, the Smart! classification, offers missing parameters,
> create/edit/delete, single- and bulk price/stock changes, bulk publish/unpublish, and reading
> promotion packages. The remaining promo write operations and media land in later releases.

## Read an offer

```java
try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.PRODUCTION)) {
    Offer offer = client.offers().get("13579");

    System.out.println(offer.name() + " — " + offer.status());
    if (offer.buyNowPrice() != null) {
        Money price = offer.buyNowPrice();
        System.out.println("Buy Now: " + price.amount() + " " + price.currency());
    }
}
```

`Offer` is an immutable record. `buyNowPrice` is present only for a fixed-price sale
(`OfferFormat.BUY_NOW` / `ADVERTISEMENT`); an auction leaves it `null`. `availableStock` is
`null` when the offer has no tracked quantity. Amounts use the shared `Money` type
(`sdk.core.Money`) — the exact server decimal string plus its ISO-4217 currency.

### Read only selected parts

When you need just the stock or the price, `getFields(...)` is a faster, lighter read than the full
`get(...)`. Request one or more `OfferPart`s; the returned `PartialOffer` populates only those.

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.OfferPart;

PartialOffer parts = client.offers().getFields("13579", OfferPart.STOCK, OfferPart.PRICE);
System.out.println("stock=" + parts.availableStock() + ", price=" + parts.price());
parts.marketplacePrices().forEach((marketplace, price) ->
        System.out.println(marketplace + ": " + price.amount() + " " + price.currency()));
```

A part you did not request is `null` (`availableStock`/`price`) or empty (`marketplacePrices`); at
least one part is required.

## List your offers

`streamOffers` returns a **lazy** `Stream<OfferSummary>`: pages are fetched from Allegro only
as you consume them, so a `limit(...)` or `findFirst()` stops the paging early.

```java
OfferFilter filter = OfferFilter.builder()
        .status(OfferStatus.ACTIVE)
        .format(OfferFormat.BUY_NOW)
        .name("keyboard")
        .build();

client.offers().streamOffers(filter)
        .limit(50)
        .forEach(offer -> System.out.println(offer.id() + " — " + offer.name()));
```

Use `OfferFilter.all()` to list every offer. `OfferSummary` is a lighter projection than
`Offer` — id, name, category, format, status, Buy Now price, available/sold stock and the
primary image URL — with the same null rules (no `buyNowPrice` for an auction, etc.).

To find offers that still need work, `streamUnfilledParameters()` lists those missing category
parameters:

```java
client.offers().streamUnfilledParameters()
        .forEach(entry -> System.out.println(
                entry.offerId() + " missing " + entry.parameterIds().size() + " parameter(s)"));
```

## Smart! classification

```java
SmartClassification smart = client.offers().smartClassification("13579");
if (!smart.fulfilled()) {
    smart.conditions().stream()
            .filter(condition -> !condition.fulfilled())
            .forEach(condition -> System.out.println("Not met: " + condition.name()));
}
```

`fulfilled()` says whether the offer currently qualifies for Allegro Smart!; `conditions()`
breaks down each requirement and whether the offer meets it.

## Bulk publish / unpublish

`offers().batch()` runs bulk commands. The SDK submits the command, waits for Allegro to
finish it, and gathers the per-offer results — you get one terminal `BatchReport` back, no
polling or futures to manage.

```java
BatchReport report = client.offers().batch().publish(List.of("13579", "24680"));
System.out.println(report.success() + "/" + report.total() + " published");
report.tasks().stream()
        .filter(task -> !"SUCCESS".equals(task.status()))
        .forEach(task -> System.out.println("failed " + task.offerId() + ": " + task.message()));
```

`unpublish(offerIds)` is the same shape. `BatchReport` carries the `total`/`success`/`failed`
counts and a `TaskResult` per offer (its `offerId`, `status`, and a `message` on failure).

The same sub-facade also does bulk price and stock changes:

```java
client.offers().batch().changePrices(List.of("13579", "24680"), Money.of("129.00", "PLN"));
client.offers().batch().changeQuantities(List.of("13579", "24680"), 50);
```

`changePrices` sets a fixed Buy Now price on every listed offer; `changeQuantities` sets their
available stock. Both return the same terminal `BatchReport`.

For mixed, per-offer changes — a different price on each marketplace, a relative adjustment, or a
price and a stock change together — use `modifyPricesAndStock`. Each `BulkPriceStockModification`
targets one offer and needs at least one price or stock change; a price change is `fixed` (set),
`gain` (add/subtract an amount) or `percentage`, a stock change is `fixed` or `gain`.

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BulkPriceStockModification;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BulkPriceStockModification.PriceChange;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BulkPriceStockModification.StockChange;

PriceStockBatchReport report = client.offers().batch().modifyPricesAndStock(List.of(
        BulkPriceStockModification.forOffer("13579")
                .price("allegro-pl", PriceChange.fixed(Money.of("129.00", "PLN")))
                .price("allegro-cz", PriceChange.percentage("-5%"))
                .stock(StockChange.fixed(50))
                .build(),
        BulkPriceStockModification.forOffer("24680")
                .stock(StockChange.gain(-3))
                .build()));

System.out.println(report.success() + "/" + report.total() + " changes applied");
```

It returns a `PriceStockBatchReport` — the same `total`/`success`/`failed` counts plus a
`PriceStockTaskResult` per changed field (its `offerId`, the `field` it touched, `status`, and a
`message` on failure).

## Apply automatic-pricing rules in bulk

An automatic-pricing rule keeps an offer's Buy Now price in step with the market (for example,
following the lowest Allegro price). `batch().applyPricingRules(...)` attaches such a rule to — or
removes it from — many offers at once on one or more marketplaces; defining the rules themselves
lives on the pricing facade. The request has two mutually exclusive modes: **assign** a rule, or
**remove** the rules. An assignment may carry an optional `PriceRange` bounding the price the rule
may set. It returns the same terminal `BatchReport`.

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchPricingRulesRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchPricingRulesRequest.PriceRange;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchPricingRulesRequest.PriceRange.CurrencyBasis;

// Assign a rule on allegro-pl, bounded between 10 and 500 PLN, to two offers.
BatchReport assigned = client.offers().batch().applyPricingRules(
        BatchPricingRulesRequest.assignRules(List.of("13579", "24680"))
                .onMarketplace("allegro-pl", "641c73feaef0a8281a3d11f8", PriceRange.of(
                        CurrencyBasis.MARKETPLACE_CURRENCY,
                        Money.of("10.00", "PLN"), Money.of("500.00", "PLN")))
                .build());

// Remove the rules on allegro-pl from one offer.
client.offers().batch().applyPricingRules(
        BatchPricingRulesRequest.removeRules(List.of("13579"))
                .fromMarketplace("allegro-pl")
                .build());
```

`onMarketplace` has an overload without the `PriceRange` when the rule needs no bound. Call it
once per marketplace to assign the rule on several; call `fromMarketplace` once per marketplace to
remove.

## Change offer settings in bulk

`batch().modify(...)` applies an offer-settings change to many offers in one command. The first
supported settings are the **listing duration** (a fixed `OfferDuration` or unlimited) and the
**dispatch time** (`HandlingTime`). A command changes **exactly one** setting — Allegro rejects a
command whose modification carries more than one element — so to change two settings submit two
commands. It returns the same terminal `BatchReport`.

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchModificationRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.HandlingTime;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.OfferDuration;

// Set a 30-day listing duration on two offers.
BatchReport report = client.offers().batch().modify(
        BatchModificationRequest.forOffers(List.of("13579", "24680"))
                .listingDuration(OfferDuration.DAYS_30)   // or .unlimitedListing()
                .build());

// A separate command changes the dispatch time.
client.offers().batch().modify(
        BatchModificationRequest.forOffers(List.of("13579", "24680"))
                .handlingTime(HandlingTime.DAYS_2)
                .build());
```

`OfferDuration`/`HandlingTime` name one clean set of durations (e.g. `DAYS_30`, `IMMEDIATE`); the
wire's redundant hour/day spellings for the same duration are hidden.

## Create an offer

```java
CreateOfferRequest request = CreateOfferRequest.builder()
        .name("Mechanical keyboard")
        .categoryId("257")
        .buyNowPrice(Money.of("199.99", "PLN"))
        .availableStock(10)
        .build();

Offer created = client.offers().create(request);
```

The required fields (name, category, Buy Now price, stock) are validated fail-fast by the
builder. The offer is created as a **draft** — publish it with
`client.offers().batch().publish(List.of(created.id()))`. Delete an unpublished draft with
`client.offers().deleteDraft(offerId)`.

### Delivery terms and after-sales conditions

An offer can reference the seller's configured **shipping-rate table** and **after-sales
conditions** (implied warranty, return policy, warranty) by id — the same ids Allegro assigns
to those templates:

```java
CreateOfferRequest request = CreateOfferRequest.builder()
        .name("Mechanical keyboard")
        .categoryId("257")
        .buyNowPrice(Money.of("199.99", "PLN"))
        .availableStock(10)
        .delivery(OfferDelivery.builder()
                .shippingRatesId("a1b2c3d4-…")   // sale delivery-settings shipping-rate id
                .handlingTime("PT24H")            // ISO-8601 duration
                .build())
        .afterSalesServices(AfterSalesServices.builder()
                .impliedWarrantyId("11111111-…")
                .returnPolicyId("22222222-…")
                .warrantyId("33333333-…")
                .build())
        .build();
```

Both blocks are optional, and both are read back on `client.offers().get(offerId)` as
`offer.delivery()` and `offer.afterSalesServices()`.

### Selling format, auction prices and stock unit

The default is a Buy Now offer counted in single units, and `buyNowPrice` is required. For an
auction, set `sellingFormat(AUCTION)` and a `startingPrice` instead — Buy Now becomes optional
(omit it for a pure auction, or set it for a buy-it-now-or-bid offer). Set `stockUnit` for offers
sold in pairs or sets:

```java
CreateOfferRequest auction = CreateOfferRequest.builder()
        .name("Vintage lamp")
        .categoryId("257")
        .availableStock(1)
        .sellingFormat(OfferFormat.AUCTION)
        .startingPrice(Money.of("1.00", "PLN"))   // required for an auction
        .minimalPrice(Money.of("150.00", "PLN"))  // optional reserve price
        .stockUnit(StockUnit.UNIT)
        .build();                                  // no buyNowPrice → a pure auction
```

The builder validates this fail-fast: an auction with no `startingPrice`, or any other format
with no `buyNowPrice`, throws. On the read side, `offer.startingPrice()`, `offer.minimalPrice()`
and `offer.stockUnit()` are populated for auctions; `offer.buyNowPrice()` is `null` for a pure
auction.

### Description and location

The standardized description is an ordered list of sections, each a group of text and image
items; the location is where the offer ships from. Both are read back on `offers().get(offerId)`:

```java
CreateOfferRequest request = CreateOfferRequest.builder()
        .name("Mechanical keyboard")
        .categoryId("257")
        .buyNowPrice(Money.of("199.99", "PLN"))
        .availableStock(10)
        .description(OfferDescription.of(
                DescriptionSection.of(DescriptionItem.text("<h1>Mechanical keyboard</h1>")),
                DescriptionSection.of(DescriptionItem.image("https://img.example/keyboard.jpg"))))
        .location(OfferLocation.builder()
                .city("Warszawa").countryCode("PL").postCode("00-001").province("mazowieckie")
                .build())
        .build();
```

An item kind Allegro adds after this SDK release reads back as `DescriptionItemType.UNKNOWN`
rather than failing the response.

### Category parameters

A category dictates which parameters an offer must carry and the shape each takes. Build each
one with the factory that matches its kind — a **dictionary** parameter with value ids chosen
from the category's dictionary, a **free-text** parameter with plain strings, or a **range**
parameter with a `from`…`to` span — and add them to the request:

```java
CreateOfferRequest request = CreateOfferRequest.builder()
        .name("Mechanical keyboard")
        .categoryId("257")
        .buyNowPrice(Money.of("199.99", "PLN"))
        .availableStock(10)
        .addParameter(OfferParameter.dictionary("11321", "1"))       // a dictionary value id
        .addParameter(OfferParameter.freeText("11324", "Cherry MX"))  // a free-text value
        .addParameter(OfferParameter.range("11325", "10", "20"))      // a numeric/date range
        .build();
```

Use `parameters(List<OfferParameter>)` to set them all at once. The value ids and their meaning
come from the category definition (see `client.categories()` in bucket E).

On the read side, `offer.parameters()` returns every parameter the offer carries. Call
`parameter.kind()` (`DICTIONARY` / `FREE_TEXT` / `RANGE`) to tell them apart rather than guessing
from which list is populated — a **dictionary** parameter is read back with both its `valuesIds`
(the ids) **and** `values` (the human-readable labels of those ids):

```java
for (OfferParameter parameter : offer.parameters()) {
    switch (parameter.kind()) {
        case DICTIONARY -> System.out.println(parameter.valuesIds() + " = " + parameter.values());
        case FREE_TEXT  -> System.out.println(parameter.values());
        case RANGE      -> System.out.println(parameter.rangeValue().lowerBound()
                                              + ".." + parameter.rangeValue().upperBound());
    }
}
```

A read parameter can be fed straight back into a create: `toRaw()` sends only the value ids of a
dictionary parameter (the labels are read-only — echoing them back is rejected by Allegro).

### Product set (productized categories)

Many categories are **productized**: an offer must be bound to a catalogue product. Add a
`ProductSetElement` referencing the product by id — a single product for a normal offer, or
several (each with its own `quantity`) for a set or multipack. A productized category also
requires the GPSR **responsible producer**, referenced by id or by name:

```java
CreateOfferRequest request = CreateOfferRequest.builder()
        .name("Wireless mouse")
        .categoryId("325040")
        .buyNowPrice(Money.of("79.99", "PLN"))
        .availableStock(10)
        .addProductSetElement(ProductSetElement.of("8f2b1c00-…-000000000001")  // product id
                .withResponsibleProducer(ResponsibleProducerRef.byId("44444444-…"))
                .withMarketedBeforeGpsrObligation(false))
        .build();
```

Use `ProductSetElement.of(productId, quantity)` for a set element, and
`ResponsibleProducerRef.byName("ACME Manufacturing")` to reference a producer by name instead of
id (the SDK writes the `oneOf` discriminator for you). On the read side, `offer.productSet()`
returns the bindings; each element's `productId()`, `quantity()`, `responsibleProducer()` (id-only
on read) and `marketedBeforeGpsrObligation()` are populated. Register responsible producers via
`client.settings()` (bucket K). This is the product-**reference** form; defining a brand-new
product inline, `responsiblePerson`, the `safetyInformation` details and `deposits` are not
modelled yet.

### External id, language and size table

An offer can also carry the seller's own **external identifier** (your system's SKU/id for the
offer), the **listing language** (BCP-47 code, e.g. `"pl-PL"`, defaults to the account language),
and the id of a **size table** to attach. All three are optional and read back on `offers().get(offerId)`:

```java
CreateOfferRequest request = CreateOfferRequest.builder()
        .name("Mechanical keyboard")
        .categoryId("257")
        .buyNowPrice(Money.of("199.99", "PLN"))
        .availableStock(10)
        .externalId("SKU-12345")
        .language("pl-PL")
        .sizeTableId("size-table-1")
        .build();
```

On the read side they surface as `offer.externalId()`, `offer.language()` and `offer.sizeTableId()`.

## Edit an offer

`edit` is a **partial** update — only the fields you set are changed; everything else keeps its
current value.

```java
Offer updated = client.offers().edit("13579", EditOfferRequest.builder()
        .name("Mechanical keyboard (2026 edition)")
        .availableStock(25)
        .build());
```

Here only the name and stock are sent; the price, images and all other fields are untouched.

## Promotion packages

`offers().promoOptions()` reads the offer promotion packages (bold title, highlight, …):

```java
AvailablePromotionPackages available = client.offers().promoOptions().availablePackages();
available.basePackages().forEach(promotionPackage ->
        System.out.println(promotionPackage.id() + " — " + promotionPackage.name()));

OfferPromoOptions applied = client.offers().promoOptions().forOffer("13579");
if (applied.basePackage() != null) {
    System.out.println("base package valid until " + applied.basePackage().validTo());
}
```

`availablePackages()` lists what can be applied (base + extra packages); `forOffer(offerId)`
shows what an offer currently has, with each package's validity window. `forAllOffers()` streams
that across every offer, and `modify(...)` applies changes to one offer:

```java
client.offers().promoOptions().forAllOffers()                 // Stream<OfferPromoOptions>
        .forEach(promo -> System.out.println(promo.offerId() + ": base=" + promo.basePackage()));

client.offers().promoOptions().modify("13579", List.of(
        PromoOptionModification.change(PromoPackageType.BASE, "pkg-1"),   // set/change the base package
        PromoOptionModification.removeNow(PromoPackageType.EXTRA, "pkg-2")));
```

To set packages across **many** offers at once, `modifyBatch(...)` runs a command (submit → poll →
gather) and returns a terminal `BatchReport`. Give a base package and/or extra packages (available
ids come from `availablePackages()`); omitting the extra packages preserves whatever the offers
already have. Choose when it takes effect with `PromoModificationTiming`.

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchPromoOptionsRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.PromoModificationTiming;

BatchReport report = client.offers().promoOptions().modifyBatch(
        BatchPromoOptionsRequest.forOffers(List.of("13579", "24680"))
                .basePackage("emphasized1d")
                .addExtraPackage("bold30d")
                .timing(PromoModificationTiming.END_OF_CYCLE)
                .build());
```

## Change the Buy Now price

```java
client.offers().changeBuyNowPrice("13579", Money.of("149.50", "PLN"));
```

The SDK issues Allegro's price-change command and returns once the change is accepted; a
rejected price surfaces as an `AllegroBadRequestException` carrying the field-level error
(e.g. `input.buyNowPrice` below the offer's minimum).

## Images and attachments

`client.offers().media()` uploads the images and document attachments an offer references.
An image is one step and yields a hosted URL to put in a `CreateOfferRequest`'s image list:

```java
OfferMedia media = client.offers().media();

OfferImage image = media.uploadImage(jpegBytes, ImageFormat.JPEG);  // or media.uploadImage(sourceUrl)
String hostedUrl = image.location();                                // use in CreateOfferRequest.imageUrls
```

An attachment (a manual, energy label, competition rules, …) is two steps — declare it, then
upload the file bytes to the returned id:

```java
OfferAttachment declared = media.createAttachment(
        AttachmentDeclaration.of(AttachmentType.USER_MANUAL, "manual.pdf"));
OfferAttachment uploaded = media.uploadAttachment(declared, pdfBytes, "application/pdf");
String fileUrl = uploaded.fileUrl();                                // hosted once the upload completes
```

Pass the whole `declared` attachment (not just its id) to `uploadAttachment` — it carries the
one-time upload URL Allegro returned, which the SDK PUTs the bytes to (the URL is single-use and
its format may change, so it is never composed by hand). `media.getAttachment(id)` reads an
attachment back. An attachment kind Allegro adds after this SDK release reads back as
`AttachmentType.UNKNOWN`.

## Offer events and operation status

`client.offers().streamEvents(...)` is a lazy, resumable feed of what happened to your offers —
activations, endings, price and stock changes, bids. Each `OfferEvent` carries its `type`, when it
`occurredAt`, and the affected `offerId`:

```java
client.offers().streamEvents(OfferEventFilter.all())          // or .ofType("OFFER_PRICE_CHANGED")
        .limit(50)
        .forEach(event -> System.out.println(event.type() + " on offer " + event.offerId()));
```

The stream pages by event id under the hood; take what you need with `limit(...)`. A create or edit
that Allegro processes asynchronously returns an operation id — poll it with
`client.offers().operationStatus(offerId, operationId)` to get an `OfferProcessingStatus`
(`PENDING` / `IN_PROGRESS` / `COMPLETED`).

## Errors

| Situation | Exception |
|---|---|
| Offer id does not exist | `AllegroNotFoundException` |
| Price below the allowed minimum / invalid | `AllegroBadRequestException` (typed field errors) |
| Missing or expired authorization | `AllegroAuthException` |
| Rate limit exceeded | `AllegroRateLimitException` (`retryAfterSeconds`) |
