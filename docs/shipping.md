# Shipping

`client.shipping()` groups the seller's logistics: shipment management, delivery
configuration, and points of service. Available today: the **points-of-service**
sub-facade (full CRUD) and the seller's **delivery methods** listing. Shipping-rate
configuration, delivery settings and shipment management (WZA) land in later
bucket-C PRs.

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

`paymentPolicy` (`IN_ADVANCE` / `CASH_ON_DELIVERY`) is a closed server enum: a
value outside the set would fail deserialization rather than mapping to a
sentinel — see `KNOWN-SERVER-BEHAVIORS.md`.

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
            .confirmationType(ConfirmationType.CONTACT_NOT_REQUIRED)
            .address(Address.builder()
                .street("Grunwaldzka 100")
                .city("Gdansk")
                .zipCode("80-244")
                .state("pomorskie")
                .countryCode("PL")
                .build())
            .openHours(List.of(
                OpenHour.builder().dayOfWeek("MONDAY").fromTime("08:00").toTime("16:00").build()))
            .externalId("store-001")   // your own identifier (optional)
            .build());

    System.out.println("created " + created.id());
}
```

`name`, `type`, `status`, `confirmationType` and `address` are required — the
builder fails fast with an `IllegalStateException` naming the missing field if
one is omitted. `Address` requires `city`, `zipCode`, `state` and `countryCode`.
Length limits (e.g. `name` ≤ 80, `zipCode` ≤ 10) are enforced locally.

## Read a point of service

```java
PointOfService point = client.shipping().points().get(pointOfServiceId);
```

Every field maps to an immutable record; enum-typed fields (`type`, `status`,
`confirmationType`) fall back to an `UNKNOWN` sentinel if Allegro returns a value
a given SDK release does not model, so reads never break on a new server value.

## List a seller's points of service

```java
List<PointOfService> points = client.shipping().points().list(sellerId);

// optionally limited to one country
List<PointOfService> polishPoints = client.shipping().points().list(sellerId, "PL");
```

`sellerId` is required (Allegro's `seller.id` query parameter). The response is
not paginated — the seller's full set arrives in one call — so this returns a
plain `List`, not a lazy `Stream`.

## Modify a point of service

```java
PointOfService updated = client.shipping().points().update(pointOfServiceId,
    PointOfServiceRequest.builder()
        .name("Pickup Point Center East")
        .type(PosType.PICKUP_POINT)
        .status(PosStatus.ACTIVE)
        .confirmationType(ConfirmationType.CONTACT_NOT_REQUIRED)
        .address(Address.builder()
            .street("Grunwaldzka 100").city("Gdansk")
            .zipCode("80-244").state("pomorskie").countryCode("PL").build())
        .openHours(List.of(
            OpenHour.builder().dayOfWeek("MONDAY").fromTime("08:00").toTime("16:00").build()))
        .build());
```

`update` replaces the point with the supplied state (PUT semantics), so build
the request from every field the point should keep, not only the ones that
change. The same required fields and length limits as `create` apply.

## Delete a point of service

```java
client.shipping().points().delete(pointOfServiceId);
```

## Notes

- **Conflict on create.** Allegro returns `409 Conflict` when a *similar* point
  of service already exists (the existing point's URL is on the `Location`
  response header). This release surfaces it as the base `AllegroException`; use
  a distinct `name` / `externalId` to avoid it.
- Errors are grouped by remediation: an invalid request throws
  `AllegroBadRequestException` with the parsed field errors, a missing point
  throws `AllegroNotFoundException`, and every exception carries the Allegro
  `traceId()` for support tickets.
