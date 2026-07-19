# Fulfillment (One Fulfillment by Allegro)

Reached via `client.fulfillment()`. One Fulfillment is Allegro's end-to-end logistics
service: enrolled sellers ship goods to Allegro's warehouse and Allegro stores stock and
fulfils their orders. All operations require an account enrolled in One Fulfillment and the
`fulfillment:read` / `fulfillment:write` OAuth scopes; a call from a non-enrolled account is
rejected with a typed error.

> **Scope.** Removal preferences, the read reports (stock, available products, parcels, refund
> dispositions), tax id and the advance-ship-notice lifecycle (create/submit/labels/receiving) are
> all available. The notice's polymorphic `shipping` declaration is the one deferred piece — see
> the note under Advance Ship Notices below.

## Removal preferences

The removal preference tells Allegro what to do with goods that must leave the warehouse —
return them to the seller (`WITHDRAWAL`, which needs a return address) or dispose of them
(`DISPOSAL`).

### Read the active preference

```java
RemovalPreference current = client.fulfillment().removalPreference();
System.out.println(current.operation()); // WITHDRAWAL or DISPOSAL
```

### Set the preference

Build the request with the fluent builders; required fields and the address length limits are
validated before the call is made.

```java
WithdrawalAddress returnAddress = WithdrawalAddress.builder()
        .company("My Store Ltd")
        .street("Testowa 1")
        .postalCode("00-001")
        .city("Warszawa")
        .countryCode("PL")
        .phone(PhoneNumber.of("48", "600100200"))
        .build();

RemovalPreference preference = RemovalPreference.builder()
        .operation(RemovalOperation.WITHDRAWAL)
        .withdrawalAddress(returnAddress)
        .build();

RemovalPreference stored = client.fulfillment().setRemovalPreference(preference);
```

For `DISPOSAL`, omit the address:

```java
client.fulfillment().setRemovalPreference(
        RemovalPreference.builder().operation(RemovalOperation.DISPOSAL).build());
```

## Stock report

`stock()` streams every stock line lazily; pages are fetched only as the stream is consumed.
`stock(StockFilter)` narrows the report. Each `StockItem` carries the product, on-hand
quantities, recent selling stats, reserve health and any storage fee — all optional, because
the server may omit them for a given line.

```java
// low-stock products, most-urgent first
try (Stream<StockItem> stock = client.fulfillment().stock(
        StockFilter.builder().outOfStockInTo(14).sort("outOfStockIn").build())) {
    stock.forEach(item -> System.out.println(
            item.product().name() + " → " + item.quantity().available()
                    + " (" + item.reserve().status() + ")"));
}
```

## Available products

`availableProducts()` streams the products the seller may ship into One Fulfillment.

```java
long count = client.fulfillment().availableProducts().count();
```

## Parcels of an order

`parcelsOf(orderId)` returns the parcels shipped from the warehouse for one order, with the
waybill and the product lines (serial numbers and expiry included).

```java
FulfillmentOrder order = client.fulfillment().parcelsOf("abc-order-123");
order.parcels().forEach(parcel -> System.out.println(parcel.waybill()));
```

## Refund dispositions

`refundDispositions(RefundDispositionFilter)` streams the report of how returned or bounced
goods were handled. The open-set fields (`type`, `stockStatus`, `accountableForNonSellability`,
the refund action state) map to forward-compatible enums that resolve to `UNKNOWN` on a value
Allegro adds later, rather than failing the read.

```java
try (Stream<RefundDisposition> report = client.fulfillment().refundDispositions(
        RefundDispositionFilter.builder()
                .createdFrom(OffsetDateTime.now().minusDays(30))
                .build())) {
    report.forEach(row -> System.out.println(row.type() + " / " + row.stockStatus()));
}
```

## Tax identification number

Read the registered number and its verification status, register a new one, or replace it.

```java
TaxId current = client.fulfillment().taxId();
client.fulfillment().addTaxId("PL1234567890");
client.fulfillment().updateTaxId("PL9876543210");
```

## Advance Ship Notices

An Advance Ship Notice (ASN) declares a shipment of goods into the warehouse. Reach the
lifecycle through `fulfillment().advanceShipNotices()`.

### Stream and read

`streamNotices()` is a lazy stream (pages fetched on demand); filter it by lifecycle status.
`get(id)` reads a single notice and captures the server's `ETag` as `version()` — you need
that token to update the notice.

```java
AdvanceShipNotices asn = client.fulfillment().advanceShipNotices();

try (Stream<AdvanceShipNotice> drafts = asn.streamNotices(
        AsnFilter.builder().addStatus(AsnStatus.DRAFT).build())) {
    drafts.forEach(notice -> System.out.println(notice.displayNumber()));
}

AdvanceShipNotice notice = asn.get("2f1e...-asn-uuid");
```

### Create, update and submit

`create(...)` returns the draft with its `version()`. Updates guard against a concurrent change
with that version as an `If-Match` token — pass the `version()` from the notice you just read or
created. `submit(...)` sends the draft to the warehouse and blocks until the async submit command
reaches a terminal `SubmitStatus`; treat `FAILED` as an unsuccessful submission.

```java
AdvanceShipNotice draft = asn.create(AsnRequest.builder()
        .addItem("product-uuid", 12)
        .handlingUnit(new HandlingUnit("BOX", BigDecimal.valueOf(2), "ONE_FULFILMENT"))
        .build());

AdvanceShipNotice updated = asn.update(draft.id(), AsnRequest.builder()
        .addItem("product-uuid", 15)
        .build(), draft.version());

SubmitStatus outcome = asn.submit(draft.id());          // or submit(id, Duration.ofMinutes(2))
if (outcome == SubmitStatus.FAILED) {
    // the warehouse rejected the notice
}
```

Once submitted, amend the notice with `updateSubmitted(id, SubmittedAsnUpdate, version)`, `cancel(id)`
it, or (for a draft) `delete(id)` it.

### Labels and receiving state

`labels(id)` downloads the printable labels as PDF bytes; `receivingState(id)` reports how far the
warehouse has got unpacking the goods.

```java
byte[] pdf = asn.labels(notice.id());
ReceivingState state = asn.receivingState(notice.id());   // stage(), content() per product
```

> The notice's polymorphic `shipping` declaration (the `COURIER_BY_SELLER` / `OWN_TRANSPORT` /
> `THIRD_PARTY_DELIVERY` variants, each with its own courier / carrier / arrival details) is not
> yet part of the read model or the write builders. It is blocked on a Layer-1 generation defect:
> the read DTO cannot deserialize those three methods (their generated subtypes extend the write
> base rather than the read base), so exposing the declaration waits on a Layer-1 regeneration.
> The `ALREADY_IN_WAREHOUSE` read method and the write body itself are unaffected.

## Errors

| Situation | Exception |
|---|---|
| Invalid request (typed field errors) | `AllegroBadRequestException` (`errors()`) |
| Token rejected | `AllegroAuthException` (after one transparent re-auth) |
| No such resource | `AllegroNotFoundException` |
| Rate limited | `AllegroRateLimitException` (`retryAfterSeconds()`) |
| Server / network fault | `AllegroServerException` |

All carry `statusCode()` and, when Allegro sent one, `traceId()`.
