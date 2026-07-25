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

Required fields (`name`, `type`, and **both** buyer-class periods `individual` and
`corporate`) are validated when you call `build()`; a missing or over-long value throws
`IllegalStateException` before any request is sent. Both periods are mandatory even though
the API spec marks neither `required` — the endpoint rejects a missing one with
`422 UNPROCESSABLE_ENTITY` (verified on the sandbox), so the builder fails fast instead.

```java
WarrantyRequest request = WarrantyRequest.builder()
        .name("2 year seller warranty")          // required, max 200 chars
        .type(WarrantyType.SELLER)                // required: SELLER or MANUFACTURER
        .individual(WarrantyPeriod.of("P24M"))    // required — ISO-8601 duration, individual buyers
        .corporate(WarrantyPeriod.lifetimeWarranty()) // required — corporate (business) buyers
        .description("Covers manufacturing defects")
        .build();

Warranty created = afterSale.createWarranty(request);
String warrantyId = created.id();
```

### Read one, or stream all

`streamWarranties()` returns a **lazy** `Stream`: pages are fetched only as you consume
them, so `findFirst()` / `limit(n)` never fetch more than they need. The stream yields
lightweight summaries — call `warranty(id)` for the full definition. The endpoint serves a
single page (it caps `offset` at 59 and `limit` at 60), so the stream yields at most the
first 60 warranties — a seller's warranty set is a small dictionary, so that is the full
list in practice.

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

## Implied warranties (rękojmia)

Implied warranties are the statutory warranty of conformity. The API mirrors the seller-warranty
shape — `streamImpliedWarranties()` (lazy, single page ≤ 60), `impliedWarranty(id)`,
`createImpliedWarranty(...)`, `updateImpliedWarranty(...)` — but the period accepts **whole years,
at least two** (`P2Y`); month-form (`P24M`) and sub-two-year (`P1Y`) values are rejected with
`422`, and there is no attachment. `name` and the `individual` period are required; validated
fail-fast at `build()`.

```java
ImpliedWarrantyRequest request = ImpliedWarrantyRequest.builder()
        .name("2 year implied warranty")             // required, max 200 chars
        .individual(ImpliedWarrantyPeriod.of("P2Y"))  // required — whole years, min two
        .corporate(ImpliedWarrantyPeriod.of("P2Y"))   // optional — whole years, min two
        .address(new AfterSalesAddress(               // optional; all fields required if set
                "Allegro sp. z o.o.", "Grunwaldzka 182", "60-166", "Poznań", "PL"))
        .description("Statutory warranty of conformity")
        .build();

ImpliedWarranty created = afterSale.createImpliedWarranty(request);
afterSale.streamImpliedWarranties().forEach(summary -> System.out.println(summary.name()));
```

## Return policies

Return policies model how a seller accepts returns. `name`, the `fulfillment` flag (fixed at
creation) and `availability` are required on create; `updateReturnPolicy(...)` takes a separate
`ReturnPolicyUpdateRequest` that omits `fulfillment` (the server rejects changing it). `options`
(the boolean return-handling flags) is **required whenever the availability range is not
`DISABLED`** — the builder rejects an enabled policy without them fail-fast, mirroring the server.
Return policies also support **delete**. `streamReturnPolicies()` returns **full** `ReturnPolicy`
records (not summaries), lazily, single page ≤ 60.

```java
ReturnPolicyRequest request = ReturnPolicyRequest.builder()
        .name("Standard 14-day returns")
        .fulfillment(false)                                   // required, fixed at creation
        .availability(ReturnPolicyAvailability.full())        // or .restricted(cause) / .disabled(cause)
        .withdrawalPeriod("P14D")                             // ISO-8601, whole days
        .returnCost(ReturnCostCoveredBy.SELLER)
        .address(new AfterSalesAddress(
                "Allegro sp. z o.o.", "Grunwaldzka 182", "60-166", "Poznań", "PL"))
        .options(new ReturnPolicyOptions(true, false, false, false, false)) // required unless DISABLED
        .build();

ReturnPolicy created = afterSale.createReturnPolicy(request);
afterSale.updateReturnPolicy(created.id(), ReturnPolicyUpdateRequest.builder()
        .name(created.name())
        .availability(ReturnPolicyAvailability.full())
        .withdrawalPeriod("P30D")
        .options(new ReturnPolicyOptions(true, false, false, false, false))
        .build());
afterSale.deleteReturnPolicy(created.id());
```

## Product compliance (GPSR)

`settings().compliance()` maintains the two GPSR dictionaries a seller attaches to offers:
responsible **persons** and responsible **producers**. Both support list (lazy, offset/limit)
and create/update; producers additionally support a single-resource read. The nested wire
`personalData`/`producerData` is flattened onto the record — `name` is the internal dictionary
label, `personName`/`tradeName` the party's own name. A `ResponsiblePartyContact` requires at
least an `email` or a `formUrl`.

```java
Compliance compliance = client.settings().compliance();

ResponsiblePersonRequest request = ResponsiblePersonRequest.builder()
        .name("Person responsible for batteries")   // required, internal label, max 50 chars
        .personName("Responsible Person Sp. z o.o.") // max 200 chars
        .address(new ResponsiblePartyAddress("PL", "Grunwaldzka 182", "60-166", "Poznań"))
        .contact(new ResponsiblePartyContact("compliance@example.com", null, null))
        .build();

ResponsiblePerson created = compliance.createResponsiblePerson(request);
compliance.streamResponsiblePersons().forEach(person -> System.out.println(person.name()));

// Producers mirror the shape (tradeName instead of personName) and add a single read:
ResponsibleProducer producer = compliance.responsibleProducer(producerId);
```

## Size tables

A size table is a small grid — named column `headers` plus `rows` of cell values — that a
seller attaches to clothing and footwear offers. Tables are created from an Allegro
template; pick one from `templates()` and reference its id.

```java
SizeTables sizeTables = client.settings().sizeTables();

// Pick a template and build a table from it.
SizeTableTemplate template = sizeTables.templates().get(0);
SizeTableRequest request = SizeTableRequest.builder()
        .name("My shoes size table")
        .templateId(template.id())
        .headers(template.headers())
        .row(List.of("M", "38", "96-104"))
        .build();

SizeTable created = sizeTables.create(request);

// Read one, list all, or update in place (update keeps the same template).
SizeTable table = sizeTables.get(created.id());
List<SizeTable> all = sizeTables.list();
SizeTable renamed = sizeTables.update(table.id(), request.toBuilder().name("Renamed").build());
```

`templateId` is required when creating a table and ignored when updating one; creating without
it fails fast with `IllegalArgumentException`.

## Tax settings

Read the VAT options available for a category — the subjects, rates (grouped by country) and
exemptions you may assign to an offer in that category. Read-only reference data:

```java
TaxSettings tax = client.settings().taxSettings(categoryId);
tax.rates().forEach(rate ->
        System.out.println(rate.countryCode() + ": " + rate.values().size() + " VAT rates"));
```

## Errors

All calls surface the SDK's remediation-grouped exceptions: `AllegroBadRequestException`
(with parsed field errors from the `errors[]` payload), `AllegroNotFoundException`,
`AllegroRateLimitException` (with `retryAfterSeconds()`), `AllegroAuthException` and
`AllegroServerException`. Reads are retried transparently; writes (POST/PUT) are not retried
by default.
