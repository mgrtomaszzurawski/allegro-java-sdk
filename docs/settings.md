# Sale settings — `client.settings()`

Seller-side configuration for your Allegro account (bucket K): after-sale service
conditions, additional services, product-compliance parties, size tables and tax settings.
Every operation is seller-scoped and uses the standard vendor media type.

> **Status:** starter slice. Only after-sale **warranties** ship today; implied warranties,
> return policies, additional services, compliance (responsible persons/producers), size
> tables and tax settings land in the following bucket-K releases.

## After-sale conditions — warranties

Reach the sub-facade through `client.settings().afterSale()`.

```java
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.AfterSaleConditions;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder.WarrantyRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.Warranty;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.WarrantyPeriod;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.WarrantyType;

AfterSaleConditions afterSale = client.settings().afterSale();
```

### Create a warranty

Required fields (`name`, `type`) are validated when you call `build()`; a missing or
over-long value throws `IllegalStateException` before any request is sent.

```java
WarrantyRequest request = WarrantyRequest.builder()
        .name("2 year seller warranty")          // required, max 200 chars
        .type(WarrantyType.SELLER)                // required: SELLER or MANUFACTURER
        .individual(WarrantyPeriod.of("P24M"))    // ISO-8601 duration for individual buyers
        .corporate(WarrantyPeriod.lifetimeWarranty())
        .description("Covers manufacturing defects")
        .build();

Warranty created = afterSale.createWarranty(request);
String warrantyId = created.id();
```

### Read one, or stream all

`streamWarranties()` returns a **lazy** `Stream`: pages are fetched only as you consume
them, so `findFirst()` / `limit(n)` never fetch more than they need. The stream yields
lightweight summaries — call `warranty(id)` for the full definition.

```java
// full definition
Warranty warranty = afterSale.warranty(warrantyId);

// lazy listing (summaries: id + name)
afterSale.streamWarranties()
        .filter(summary -> summary.name().startsWith("2 year"))
        .forEach(summary -> System.out.println(summary.id()));
```

### Update a warranty

```java
Warranty updated = afterSale.updateWarranty(warrantyId,
        request.toBuilder().description("Updated terms").build());
```

## Errors

All calls surface the SDK's remediation-grouped exceptions: `AllegroBadRequestException`
(with parsed field errors from the `errors[]` payload), `AllegroNotFoundException`,
`AllegroRateLimitException` (with `retryAfterSeconds()`), `AllegroAuthException` and
`AllegroServerException`. Reads are retried transparently; writes (POST/PUT) are not retried
by default.
