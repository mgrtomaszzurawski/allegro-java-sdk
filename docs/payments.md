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
        .forEach(op -> System.out.println(op.occurredAt() + "  " + op.type()
                + "  " + (op.value() == null ? "-" : op.value().amount())));
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
