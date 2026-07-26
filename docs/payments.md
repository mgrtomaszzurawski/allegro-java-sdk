# Payments

Payment operations, refunded payments, and refund initiation for the authenticated seller,
reached through `AllegroClient.payments()`.

## Authentication

The read streams need a **user-context token** with `payments:read`; `refund(...)` needs
`payments:write`.

## Payment operations history

`streamOperations` returns a **lazy** `Stream<PaymentOperation>` — a dated money movement on the
payment wallet (income, outcome, refund, blockade). Filter with a `PaymentOperationFilter`.

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder.PaymentOperationFilter;

client.payments().streamOperations(PaymentOperationFilter.builder().group("REFUND").build())
        .limit(100)
        .forEach(operation -> System.out.println(operation.occurredAt() + "  " + operation.type()
                + "  " + (operation.value() == null ? "-" : operation.value().amount())));
```

## Refunded payments

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder.RefundFilter;

client.payments().streamRefunds(RefundFilter.builder().orderId(orderId).build())
        .forEach(refund -> System.out.println(refund.id() + "  " + refund.status()));
```

## Initiate a refund

A refund needs the payment id, the order id, an **idempotency `commandId`**, and a
`RefundReason`. Reuse the same `commandId` to retry an interrupted refund safely — the server
treats a repeat as the same operation, not a second refund. The payment and order ids must be
UUIDs; a bad id fails fast at `build()`.

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder.RefundRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.model.PaymentRefund;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.model.RefundReason;
import java.util.UUID;

PaymentRefund refund = client.payments().refund(RefundRequest.builder()
        .paymentId(paymentId)
        .orderId(orderId)
        .commandId(UUID.randomUUID().toString())
        .reason(RefundReason.COMPLAINT)
        .build());

System.out.println("Refund " + refund.id() + " is " + refund.status());
```

### Partial refunds

With only the required fields, `refund(...)` refunds the whole payment. To refund **part** of a
payment, add the components to return; anything left unset is not refunded. A component is one of:

| Component | How to add | Refunds |
|---|---|---|
| Line item, by amount | `.lineItem(RefundLineItem.byAmount(lineItemId, Money))` | a fixed `Money` amount of one line item |
| Line item, by quantity | `.lineItem(RefundLineItem.byQuantity(lineItemId, BigDecimal))` | a number of units of one line item (server-priced) |
| Deposit | `.deposit(RefundDeposit.of(lineItemId, Money))` | a line item's deposit (e.g. packaging), by total value |
| Surcharge | `.surcharge(RefundSurcharge.of(surchargeId, Money))` | a payment surcharge, by value |
| Delivery | `.delivery(Money)` | the delivery cost |
| Overpaid | `.overpaid(Money)` | an overpaid amount |
| Additional services | `.additionalServices(Money)` | additional-service charges |

Add `.sellerComment(String)` for an optional justification. Each id must be a UUID and each `Money`
is required for its component — a bad id or a missing amount fails fast at construction, before the
request is built.

```java
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.payments.builder.RefundLineItem;

PaymentRefund refund = client.payments().refund(RefundRequest.builder()
        .paymentId(paymentId)
        .orderId(orderId)
        .commandId(UUID.randomUUID().toString())
        .reason(RefundReason.PRODUCT_NOT_AVAILABLE)
        .lineItem(RefundLineItem.byAmount(lineItemId, Money.of("19.99", "PLN")))
        .delivery(Money.of("9.90", "PLN"))
        .sellerComment("One item out of stock")
        .build());
```

The returned `PaymentRefund` echoes back the breakdown the server actually applied:
`refund.lineItems()` / `deposits()` / `surcharges()` (each a small read record carrying its id and
`Money` amount — a line item is either by amount or by quantity) plus `delivery()` / `overpaid()` /
`additionalServices()` (each a `Money`, or `null` when not refunded) and `sellerComment()`. For a
full refund those collections are empty and the amounts `null`.
