# Account & meta (`client.marketplaces()`, `client.user()`, …)

Bucket D groups the account-level and platform-metadata endpoints: the
authenticated user, public user ratings, marketplaces, auctions/bidding,
affiliate conversions and charity search. This guide grows as the bucket lands;
today it covers the marketplace listing and the current-user slice.

## Marketplaces

Marketplace metadata (available languages, currencies and shipping countries per
marketplace) is **public** — it needs no user-context token and works with an
app-only client-credentials grant.

```java
try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.PRODUCTION)) {
    for (Marketplace marketplace : client.marketplaces().list()) {
        System.out.println(marketplace.id()
                + " base=" + marketplace.baseCurrency()
                + " ships to " + marketplace.shippingCountries());
    }
}
```

`Marketplace` flattens the nested API objects to the code strings a consumer
actually uses:

| Field | Meaning |
|---|---|
| `id()` | marketplace id, e.g. `allegro-pl` |
| `offerCreationLanguages()` | BCP-47 codes an offer may be written in |
| `offerDisplayLanguages()` | BCP-47 codes a buyer may see the offer in |
| `baseCurrency()` | ISO-4217 base currency code (`null` if the marketplace declares none) |
| `additionalCurrencies()` | other accepted ISO-4217 codes |
| `shippingCountries()` | ISO country codes the marketplace ships to |

Collections are never `null` (empty when absent), so callers can iterate without
null checks.

## Current user

```java
CurrentUser me = client.user().me();
System.out.println("Logged in as " + me.login() + " (id " + me.id() + ")");
```

`user().me()` requires a **user-context** token (authorization-code or device
grant); an app-only client-credentials token is limited to public data such as
the marketplace listing above.
