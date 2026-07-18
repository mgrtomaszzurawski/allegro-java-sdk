# Orders

Orders, payments and billing for the authenticated seller, reached through
`AllegroClient.orders()`. An Allegro *order* is a **checkout form**: who bought, what they
bought, the amount due, and the buyer- and seller-side statuses.

> **Scope of this guide.** This is the starter slice of the orders domain (bucket B) and
> documents `orders().get(id)`. Order listing and filtering, seller-status updates, parcel
> tracking, invoices, customer returns, commission refunds, payments and billing follow in the
> bucket body; this page grows with them.

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
| `messageToSeller` | `String` (nullable) | buyer's note |
| `marketplaceId` | `String` (nullable) | marketplace the order was placed on |
| `updatedAt` | `OffsetDateTime` (nullable) | last modification time |
| `revision` | `String` (nullable) | optimistic-concurrency token for later status writes |

`OrderStatus` (buyer-side) is distinct from `SellerStatus` (the value the seller advances as it
prepares and ships the order). Monetary amounts use the SDK-wide
[`Money`](../allegro-client/src/main/java/io/github/mgrtomaszzurawski/allegro/sdk/core/Money.java)
type — an exact decimal string plus currency, never a lossy `double`.
