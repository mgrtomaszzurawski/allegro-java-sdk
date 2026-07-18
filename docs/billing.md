# Billing

The seller's billing ledger, reached through `AllegroClient.billing()` — the dated charges and
credits on the account, plus the dictionary that names each charge type.

## Authentication

`streamEntries(...)` needs a **user-context token** with the `billing:read` scope. `types()` is a
public dictionary and works with an app-only client-credentials token.

## Stream billing entries

`streamEntries` returns a **lazy** `Stream<BillingEntry>` — pages are fetched only as the consumer
advances. Narrow the result with a `BillingFilter` (all fields optional; `BillingFilter.all()`
streams everything).

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.billing.builder.BillingFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.billing.model.BillingEntry;
import java.time.OffsetDateTime;

BillingFilter lastMonth = BillingFilter.builder()
        .occurredFrom(OffsetDateTime.now().minusDays(30))
        .build();

client.billing().streamEntries(lastMonth)
        .limit(200)
        .forEach(entry -> System.out.println(entry.occurredAt()
                + "  " + entry.typeName()
                + "  " + (entry.value() == null ? "-" : entry.value().amount())));
```

Each `BillingEntry` exposes `id`, `occurredAt`, `typeId`/`typeName`, the signed `value` and running
`balance` (both `Money`, nullable), and the related `offerId` / `orderId`.

## Billing types

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.billing.model.BillingType;

for (BillingType type : client.billing().types()) {
    System.out.println(type.id() + " → " + type.description());
}
```
