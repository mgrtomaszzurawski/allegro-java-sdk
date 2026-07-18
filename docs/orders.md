# Orders

Orders, payments and billing for the authenticated seller, reached through
`AllegroClient.orders()`. An Allegro *order* is a **checkout form**: who bought, what they
bought, the amount due, and the buyer- and seller-side statuses.

> **Scope of this guide.** Covers the order-management surface of bucket B: reading and listing
> orders, advancing the seller status, serial numbers, parcel tracking, the order event log, and
> the carrier / pickup-point dictionaries. Invoices, customer returns, commission refunds,
> payments and billing follow in later slices; this page grows with them.

## Authentication

Order data is user-scoped: the client must hold a **user-context token** (authorization-code or
device grant) with the `orders:read` scope. An app-only client-credentials token cannot read
orders.

## Fetch a single order

```java
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.Order;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.LineItem;

try (AllegroClient client = AllegroClient.create(credentials, environment)) {
    Order order = client.orders().get("a8f6c3e2-1111-2222-3333-444455556666");

    System.out.println("Status:  " + order.status());        // e.g. READY_FOR_PROCESSING
    System.out.println("Seller:  " + order.sellerStatus());  // e.g. NEW, or null if unset
    System.out.println("Buyer:   " + order.buyer().login());
    System.out.println("To pay:  " + order.totalToPay().amount()
            + " " + order.totalToPay().currency());

    for (LineItem item : order.lineItems()) {
        System.out.println(item.quantity() + " x " + item.offerName()
                + " @ " + item.price().amount() + " " + item.price().currency());
    }
}
```

If no order with that id exists for the seller, `get` throws `AllegroNotFoundException`.

## The `Order` record

| Field | Type | Notes |
|---|---|---|
| `id` | `String` | order (checkout form) identifier |
| `status` | `OrderStatus` | buyer-side lifecycle: `BOUGHT`, `FILLED_IN`, `READY_FOR_PROCESSING`, `CANCELLED` |
| `sellerStatus` | `SellerStatus` (nullable) | seller handling status: `NEW`, `PROCESSING`, `READY_FOR_SHIPMENT`, `READY_FOR_PICKUP`, `SENT`, `PICKED_UP`, `CANCELLED`, `SUSPENDED`, `RETURNED` |
| `buyer` | `Buyer` | id, login, email, name, company, guest flag, phone |
| `lineItems` | `List<LineItem>` | id, offer id/name, quantity, unit `price`, `boughtAt` |
| `totalToPay` | `Money` | amount + ISO-4217 currency the buyer pays |
| `payment` | `OrderPayment` (nullable) | main payment: id, `type`, `provider`, `finishedAt`, `paidAmount`, feature flags — `null` while unpaid |
| `surcharges` | `List<OrderPayment>` | additional charges beyond the main payment; never `null`, possibly empty |
| `messageToSeller` | `String` (nullable) | buyer's message to the seller |
| `sellerNote` | `String` (nullable) | the seller's own private note on the order |
| `marketplaceId` | `String` (nullable) | marketplace the order was placed on |
| `updatedAt` | `OffsetDateTime` (nullable) | last modification time |
| `revision` | `String` (nullable) | optimistic-concurrency token for later status writes |

`OrderStatus` (buyer-side) is distinct from `SellerStatus` (the value the seller advances as it
prepares and ships the order). Monetary amounts use the SDK-wide
[`Money`](../allegro-client/src/main/java/io/github/mgrtomaszzurawski/allegro/sdk/core/Money.java)
type — an exact decimal string plus currency, never a lossy `double`.

### Payment

`order.payment()` is the main payment on the order (`null` until the buyer pays); `order.surcharges()`
lists any additional charges. Both are `OrderPayment` records — `type` (`PaymentType`: cash on
delivery, wire transfer, online, split payment, extended term), `provider` (`PaymentProvider`: PayU,
P24, Allegro Finance, offline, EPT), `finishedAt`, and the `paidAmount` as `Money`. Every field is
nullable because an unpaid order may carry a payment reference with nothing filled in yet.

```java
if (order.payment() != null && order.payment().type() == PaymentType.CASH_ON_DELIVERY) {
    // collect on delivery
}
```

## List and stream orders

`streamOrders` returns a **lazy** `Stream<Order>`: pages are fetched from Allegro only as the
consumer advances, and only one page is held in memory. Build an `OrderFilter` to narrow the
result (all fields optional; `OrderFilter.all()` streams everything).

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.OrderFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.SellerStatus;
import java.time.OffsetDateTime;

OrderFilter toShip = OrderFilter.builder()
        .fulfillmentStatuses(SellerStatus.READY_FOR_SHIPMENT)
        .updatedFrom(OffsetDateTime.now().minusDays(7))
        .build();

client.orders().streamOrders(toShip)
        .limit(50)                       // only the first 50 are fetched
        .forEach(order -> ship(order));
```

Never call a `listAll()` — the stream is the whole API; bound it with `limit(...)` or a
short-circuiting terminal operation.

## Advance the seller status

`markStatus` moves an order through the seller-side lifecycle. To guard against a concurrent
change, pass the `revision` you last read from the order (optimistic concurrency); omit it for
last-write-wins.

```java
Order order = client.orders().get(orderId);
String revision = order.revision();
if (revision != null) {
    client.orders().markStatus(orderId, SellerStatus.SENT, revision); // optimistic-concurrency guarded
} else {
    client.orders().markStatus(orderId, SellerStatus.SENT);           // last-write-wins
}
```

A stale revision fails with `AllegroBadRequestException` — re-read the order and retry. Passing a
blank `revision` to the three-argument overload is rejected up front (`IllegalArgumentException`)
rather than silently degrading to last-write-wins; use the two-argument overload when you do not
need the guard.

## Serial numbers and billing documents

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.SerialNumbersRequest;

client.orders().setSerialNumbers(orderId, SerialNumbersRequest.builder()
        .lineItem(lineItemId, "SN-0001", "SN-0002")
        .build());

client.orders().attachBillingDocumentLink(orderId, "https://example.com/invoice-1.pdf");
```

## Parcel tracking

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.ShipmentRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.Carrier;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.Waybill;

Carrier carrier = client.orders().carriers().get(0);           // dictionary of carriers
Waybill added = client.orders().addTrackingNumber(orderId, ShipmentRequest.builder()
        .carrierId(carrier.id())
        .waybill("00123456789")
        .build());

client.orders().trackingNumbers(orderId);                       // waybills on an order
client.orders().carrierTracking(carrier.id(), added.waybill()); // carrier delivery history
```

## Order events and pickup points

The order **event log** is a cursor stream — resume from the last event you saw, and use
`eventStats()` to discover the newest event id first.

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.OrderEventFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.PointsFilter;

client.orders().eventStats();                                   // latest event marker
client.orders().streamEvents(OrderEventFilter.all()).limit(100).forEach(this::handle);

client.orders().allegroPickupPoints(PointsFilter.ofCarriers("UPS"));
```

## Invoices, customer returns, and commission refunds

Three sub-facades hang off `orders()`.

**Invoices** — declaring an invoice is two steps: register the metadata, then upload the bytes.

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.InvoiceDeclaration;

String invoiceId = client.orders().invoices().declare(orderId, InvoiceDeclaration.builder()
        .invoiceNumber("FV/2026/01")
        .fileName("invoice.pdf")
        .build());
client.orders().invoices().uploadFile(orderId, invoiceId, pdfBytes);
client.orders().invoices().ofOrder(orderId);          // list registered invoices
```

**Customer returns (BETA)** — browse buyer returns and reject a refund:

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.RejectionRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.ReturnFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.ReturnRejectionCode;

client.orders().returns().streamReturns(ReturnFilter.builder().orderId(orderId).build());
client.orders().returns().rejectRefund(customerReturnId, RejectionRequest.builder()
        .code(ReturnRejectionCode.ITEM_FIXED)
        .reason("Repaired under warranty")
        .build());
```

**Commission refunds** — reclaim the sales commission on a cancelled line item:

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.ClaimFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder.RefundClaimRequest;

String claimId = client.orders().commissionRefunds().claim(RefundClaimRequest.builder()
        .lineItemId(lineItemId)
        .quantity(1)
        .build());
client.orders().commissionRefunds().streamClaims(ClaimFilter.all());
client.orders().commissionRefunds().cancel(claimId);
```
