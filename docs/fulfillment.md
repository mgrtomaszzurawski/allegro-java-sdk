# Fulfillment (One Fulfillment by Allegro)

Reached via `client.fulfillment()`. One Fulfillment is Allegro's end-to-end logistics
service: enrolled sellers ship goods to Allegro's warehouse and Allegro stores stock and
fulfils their orders. All operations require an account enrolled in One Fulfillment and the
`fulfillment:read` / `fulfillment:write` OAuth scopes; a call from a non-enrolled account is
rejected with a typed error.

> **Preview.** This bucket lands incrementally. The starter slice below (removal preferences)
> ships first; advance ship notices, stock, available products, parcels, refund dispositions
> and tax id follow.

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

## Errors

| Situation | Exception |
|---|---|
| Invalid request (typed field errors) | `AllegroBadRequestException` (`errors()`) |
| Token rejected | `AllegroAuthException` (after one transparent re-auth) |
| No such resource | `AllegroNotFoundException` |
| Rate limited | `AllegroRateLimitException` (`retryAfterSeconds()`) |
| Server / network fault | `AllegroServerException` |

All carry `statusCode()` and, when Allegro sent one, `traceId()`.
