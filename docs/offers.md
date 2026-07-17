# Offers

The offer lifecycle, reached from `client.offers()`.

> Starter slice — a single-offer read and a single-offer price change. Creating and editing
> offers, bulk `batch()` commands, promotion options and media land in later releases.

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
