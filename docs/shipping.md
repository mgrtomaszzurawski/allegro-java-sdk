# Shipping

`client.shipping()` groups the seller's logistics: shipment management, delivery
configuration, and points of service. This starter slice ships the
**points-of-service** sub-facade; the remaining shipping operations land in the
bucket's volume PR.

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
