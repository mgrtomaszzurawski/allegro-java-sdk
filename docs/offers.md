# Offers

The offer lifecycle, reached from `client.offers()`.

> Available now: reading a single offer, listing your offers, the Smart! classification, and a
> single-offer price change. Creating and editing offers, bulk `batch()` commands, promotion
> options and media land in later releases.

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

## Change the Buy Now price

```java
client.offers().changeBuyNowPrice("13579", Money.of("149.50", "PLN"));
```

The SDK issues Allegro's price-change command and returns once the change is accepted; a
rejected price surfaces as an `AllegroBadRequestException` carrying the field-level error
(e.g. `input.buyNowPrice` below the offer's minimum).

## Errors

| Situation | Exception |
|---|---|
| Offer id does not exist | `AllegroNotFoundException` |
| Price below the allowed minimum / invalid | `AllegroBadRequestException` (typed field errors) |
| Missing or expired authorization | `AllegroAuthException` |
| Rate limit exceeded | `AllegroRateLimitException` (`retryAfterSeconds`) |
