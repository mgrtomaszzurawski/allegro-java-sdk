# Shipping

`client.shipping()` groups the seller's logistics: shipment management, delivery
configuration, and points of service. Available today: the **points-of-service**
sub-facade (full CRUD), the seller's **delivery methods** listing, **delivery
settings** (`settings()`) and **shipping rates** (`rates()`). Shipment management
(WZA) lands in a later bucket-C PR.

## List delivery methods

```java
List<DeliveryMethod> methods = client.shipping().deliveryMethods();
for (DeliveryMethod method : methods) {
    System.out.println(method.id() + " " + method.name() + " " + method.paymentPolicy());
}
```

Returns the delivery methods Allegro offers the seller — each method's `id` is
what a shipping-rate row references to price it. The call is read-only and works
with an application (client-credentials) token; no user-context scope is needed.
The response is not paginated, so it is a plain `List`.

`paymentPolicy` (`IN_ADVANCE` / `CASH_ON_DELIVERY`) is read-only and fail-soft:
a value this SDK release does not model maps to `PaymentPolicy.UNKNOWN` rather
than breaking the read, and `null` means the server omitted it.

Points of service are a seller's personal-collection locations (click &amp;
collect). They require a user-context token with `sale:settings:read` (to read)
and `sale:settings:write` (to create or delete).

## Create a point of service

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.*;
import java.util.List;

try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {

    PointOfService created = client.shipping().points().create(
        PointOfServiceRequest.builder()
            .name("Pickup Point Center")
            .type(PosType.PICKUP_POINT)
            .status(PosStatus.ACTIVE)
            .confirmationType(ConfirmationType.AWAIT_CONTACT)
            .address(Address.builder()
                .street("Grunwaldzka 100")
                .city("Gdansk")
                .zipCode("80-244")
                .state("pomorskie")
                .countryCode("PL")
                .coordinates(new Coordinates(54.372158, 18.638306))
                .build())
            .openHours(List.of(
                OpenHour.builder().dayOfWeek("MONDAY")
                    .fromTime("08:00:00.000").toTime("16:00:00.000").build()))
            .externalId("store-001")   // your own identifier (optional)
            .build());

    System.out.println("created " + created.id());
}
```

`name`, `type`, `status`, `confirmationType` and `address` are required — the
builder fails fast with an `IllegalStateException` naming the missing field if
one is omitted. `Address` requires `city`, `zipCode`, `state`, `countryCode` and
`coordinates`. Length limits (e.g. `name` ≤ 80, `zipCode` ≤ 10) are enforced
locally. Opening-hours times use the ISO `HH:mm:ss.SSS` format. You do **not**
pass your own seller id — the SDK resolves it from the token and adds it to the
request. (The live endpoint requires `seller.id` and `coordinates` even though
the spec marks them optional — see `KNOWN-SERVER-BEHAVIORS.md`.)

## Read a point of service

```java
PointOfService point = client.shipping().points().get(pointOfServiceId);
```

Every field maps to an immutable record; enum-typed fields (`type`, `status`,
`confirmationType`) fall back to an `UNKNOWN` sentinel if Allegro returns a value
a given SDK release does not model, so reads never break on a new server value.

## List a seller's points of service

```java
List<PointOfService> points = client.shipping().points().list();

// optionally limited to one country
List<PointOfService> polishPoints = client.shipping().points().list("PL");
```

Allegro requires the `seller.id` query parameter; the SDK resolves it from the
token, so you don't pass your own id. The response is not paginated — the
seller's full set arrives in one call — so this returns a plain `List`, not a
lazy `Stream`.

## Modify a point of service

```java
PointOfService updated = client.shipping().points().update(pointOfServiceId,
    PointOfServiceRequest.builder()
        .name("Pickup Point Center East")
        .type(PosType.PICKUP_POINT)
        .status(PosStatus.ACTIVE)
        .confirmationType(ConfirmationType.AWAIT_CONTACT)
        .address(Address.builder()
            .street("Grunwaldzka 100").city("Gdansk")
            .zipCode("80-244").state("pomorskie").countryCode("PL")
            .coordinates(new Coordinates(54.372158, 18.638306)).build())
        .openHours(List.of(
            OpenHour.builder().dayOfWeek("MONDAY")
                .fromTime("08:00:00.000").toTime("16:00:00.000").build()))
        .build());
```

`update` replaces the point with the supplied state (PUT semantics), so build
the request from every field the point should keep, not only the ones that
change. The same required fields and length limits as `create` apply.

## Delete a point of service

```java
client.shipping().points().delete(pointOfServiceId);
```

## Delivery settings

`client.shipping().settings()` reads and updates the seller's delivery
configuration for their default marketplace: the domestic and cross-border
free-delivery thresholds and the join policy (how a multi-item order's delivery
cost is combined). Reads need `sale:settings:read`, updates
`sale:settings:write`.

```java
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;

DeliverySettingsView current = client.shipping().settings().get();
System.out.println(current.joinPolicy() + " free-from " + current.freeDelivery());

// update has PUT semantics — send the full desired state
DeliverySettingsView updated = client.shipping().settings().update(
    DeliverySettingsRequest.builder()
        .joinPolicy(JoinStrategy.SUM)                 // MIN / MAX / SUM (required)
        .freeDelivery(Money.of("200.00", "PLN"))      // optional threshold
        .abroadFreeDelivery(Money.of("500.00", "PLN"))
        .build());
```

Amounts are the SDK-wide `Money` (`{amount, currency}`). `joinPolicy` is required
on the request; the thresholds and marketplace are optional. On read,
`joinPolicy` falls back to `JoinStrategy.UNKNOWN` for a value this release does
not model.

## Shipping rates

`client.shipping().rates()` manages the seller's shipping-rate sets — a set
groups per-delivery-method price rows a seller attaches to their offers.
`list()` returns lightweight summaries; `get(id)` returns the full set with its
rate rows.

```java
List<ShippingRateSetSummary> sets = client.shipping().rates().list();
ShippingRateSet set = client.shipping().rates().get(sets.get(0).id());
for (ShippingRate rate : set.rates()) {
    System.out.println(rate.deliveryMethodId() + " " + rate.firstItemRate());
}
```

Create or replace a set (PUT semantics on `update`):

```java
ShippingRateSet created = client.shipping().rates().create(
    ShippingRateSetRequest.builder()
        .name("Domestic rates")
        .type(RateSetType.PHYSICAL)
        .dispatchCountry("PL")
        .rates(List.of(
            ShippingRate.builder()
                .deliveryMethodId(methodId)            // from deliveryMethods()
                .firstItemRate(Money.of("12.99", "PLN"))
                .nextItemRate(Money.of("2.00", "PLN"))
                .maxQuantityPerPackage(10)
                .maxPackageWeight(new Weight("30.0", "KILOGRAMS"))  // optional
                .build()))
        .build());
```

A rate row requires the delivery method, the first- and next-item rates and the
maximum quantity per package; the package-weight limit and dispatch-time range
are optional. A rate-set request requires a `name` and at least one rate. Each
row's `deliveryMethodId` is an id from `deliveryMethods()`. `RateSetType` reads
fail-soft (`UNKNOWN`) and writes strict.

## Shipment management (Wysyłam z Allegro)

Create a carrier shipment, read it back, cancel it, and render its documents.
Creation and cancellation are asynchronous on Allegro's side — the SDK submits
the command and polls it to a terminal state internally, so the calls are plain
blocking methods with no command handle. Each has an optional
`Duration` timeout overload.

```java
Shipment shipment = client.shipping().createShipment(
        ShipmentRequest.builder()
                .credentialsId(credentialsId)                 // carrier account; or from a delivery proposal
                .sender(PostalAddress.builder()
                        .street("Grunwaldzka 100").postalCode("80-244").city("Gdansk")
                        .email("sender@example.com").phone("+48500100100").build())
                .receiver(PostalAddress.builder()
                        .street("Marszalkowska 1").postalCode("00-001").city("Warszawa")
                        .email("receiver@example.com").phone("+48500200200").build())
                .packages(List.of(ShipmentPackage.builder()
                        .type(PackageType.PACKAGE)
                        .lengthCm(new BigDecimal("30")).widthCm(new BigDecimal("20"))
                        .heightCm(new BigDecimal("10")).weightKg(new BigDecimal("2.5"))
                        .build()))
                .labelFormat(LabelFormat.PDF)                 // optional
                .build());

Shipment fetched = client.shipping().getShipment(shipment.id());

byte[] label = client.shipping().labels(LabelRequest.builder()
        .shipmentIds(List.of(shipment.id()))
        .pageSize(LabelPageSize.A4)                           // optional
        .build());
byte[] protocol = client.shipping().protocol(shipment.id());

client.shipping().cancelShipment(shipment.id());
```

A shipment request requires a sender, a receiver and at least one package; each
package needs its type and its centimetre dimensions and kilogram weight. The
`labels(...)` and `protocol(...)` calls return the rendered documents as raw
`byte[]` (PDF or ZPL, per the shipment's format) — write them to a file or a
print stream. `createShipment` reads the created shipment back once the command
succeeds; a command that ends in a non-success state throws.

## Notes

- **Conflict on create.** Allegro returns `409 Conflict` when a *similar* point
  of service already exists (the existing point's URL is on the `Location`
  response header). This release surfaces it as the base `AllegroException`; use
  a distinct `name` / `externalId` to avoid it.
- Errors are grouped by remediation: an invalid request throws
  `AllegroBadRequestException` with the parsed field errors, a missing point
  throws `AllegroNotFoundException`, and every exception carries the Allegro
  `traceId()` for support tickets.
