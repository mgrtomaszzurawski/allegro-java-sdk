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

## Additional services

`settings().additionalServices()` reads the additional-services a seller offers on their listings:
the **definition catalog** available to the seller (grouped by category, each definition carrying a
`maxPrice`), the seller's own **groups** (lazy list + single read; each group holds services, and
each service its priced `configurations` with a country/delivery `constraint`), and a group's
**translations** (one `GroupTranslation` per language, `MANUAL` or `AUTO`). Group create/update and
translation writes ship in a follow-up slice.

```java
AdditionalServices additional = client.settings().additionalServices();

additional.categoryDefinitions()                     // catalog: categories -> definitions (+ maxPrice)
        .forEach(category -> System.out.println(category.name()));

additional.streamGroups().forEach(group ->           // lazy Stream<AdditionalServicesGroup>
        System.out.println(group.name() + " (" + group.services().size() + " services)"));

AdditionalServicesGroup group = additional.group(groupId);
GroupTranslations translations = additional.translations(groupId);
```

## Errors

All calls surface the SDK's remediation-grouped exceptions: `AllegroBadRequestException`
(with parsed field errors from the `errors[]` payload), `AllegroNotFoundException`,
`AllegroRateLimitException` (with `retryAfterSeconds()`), `AllegroAuthException` and
`AllegroServerException`. Reads are retried transparently; writes (POST/PUT) are not retried
by default.
