# Allegro Java SDK

A typed, hand-crafted Java client library for the [Allegro REST API](https://developer.allegro.pl/documentation/).

> **Status: preview (`0.1.0-preview`).** Under active development. The public API is not yet stable.

> **Unofficial and unaffiliated.** This is an independent, community-built SDK. It is **not** an
> official Allegro product and is **not affiliated with, endorsed, sponsored, or supported by
> Allegro**. "Allegro" is a trademark of Allegro.pl sp. z o.o.; the name is used here solely to
> identify the third-party REST API this library targets (nominative use). The
> software is provided "as is" under AGPL-3.0-only, without warranty of any kind. You are responsible
> for using it in accordance with [Allegro's API terms and conditions](https://developer.allegro.pl/about).

## About

This SDK provides a premium, strongly-typed surface over Allegro's REST API: a single
`AllegroClient` entry point exposing domain accessors (offers, orders, fulfillment, billing, …),
OAuth2 authentication across all three Allegro flows (authorization-code, device, client-credentials),
resilient transport with a configurable retry policy, and lazy `Stream`-based pagination.

Data-transfer types are generated from Allegro's official
[OpenAPI 3.0 specification](https://developer.allegro.pl/swagger.yaml); the client, transport,
authentication, and domain facades are hand-written.

## Architecture

Three layers, enforced by the Java Platform Module System (JPMS):

| Layer | Module | Exported | Contents |
|---|---|---|---|
| 3 — public API | `allegro-client` (`io.github…allegro`) | yes | `AllegroClient`, `sdk.domain.*` facades + builders + immutable models, `config`, `core`, `exception` |
| 2 — internal | `allegro-client` (`…allegro.internal`) | no | `*Impl` endpoint wrappers, `HttpRuntime`, `ApiPaths`, OAuth runtime |
| 1 — generated | `allegro-rest-models` | no | `*Raw` OpenAPI POJOs |

Consumers depend only on `sdk.domain.*` — never on `internal.*`, `*Raw`, or transport types.

## Documentation

- [`docs/offers.md`](docs/offers.md) — offers: read an offer, change the Buy Now price
- [`docs/orders.md`](docs/orders.md) — orders: listing, events, seller status, tracking, dictionaries
- [`docs/payments.md`](docs/payments.md) — payment operations, refunded payments, refund initiation
- [`docs/billing.md`](docs/billing.md) — billing entries and the billing-type dictionary
- [`docs/shipping.md`](docs/shipping.md) — shipping domain (points of service; more to come)
- [`docs/contacts.md`](docs/contacts.md) — seller contact cards
- [`docs/messaging.md`](docs/messaging.md) — message center: threads, messages, attachments
- [`docs/disputes.md`](docs/disputes.md) — post-purchase issues: disputes and claims (read)
- `docs/<domain>.md` — per-domain usage guides, added as each
  domain lands
  - [`docs/account.md`](docs/account.md) — account & meta: marketplaces, current user (bucket D)
  - [`docs/catalog.md`](docs/catalog.md) — categories, products, compatibility (`client.catalog()`)
  - [`docs/offers-extras.md`](docs/offers-extras.md) — classifieds, tags, translations,
    rating, and bundles (bucket F)
  - [`docs/pricing.md`](docs/pricing.md) — automatic pricing rules (promotions, deposits to follow)
  - [`docs/fulfillment.md`](docs/fulfillment.md) — One Fulfillment by Allegro (removal preferences, …)
- [`docs/campaigns.md`](docs/campaigns.md) — badge campaigns, Allegro Prices, AlleDiscount
- [`docs/settings.md`](docs/settings.md) — seller sale settings (`client.settings()`): after-sale
  warranties (more of bucket K to follow)

## Quick start

```java
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;

// Console/headless app: the user confirms once at a verification URL,
// the SDK handles polling, token caching, proactive refresh, and rotation.
var credentials = DeviceCodeCredentials.of(clientId, clientSecret,
        auth -> System.out.println("Confirm at: " + auth.verificationUriComplete()));

try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
    var currentUser = client.user().me();
    System.out.println("Logged in as " + currentUser.login());

    // Persist this and pass it to DeviceCodeCredentials.ofRefreshToken(...)
    // next time - no prompt ever again.
    String refreshToken = client.refreshToken();
}
```

Errors are typed by remediation (`AllegroBadRequestException` with field-level
`errors()`, `AllegroRateLimitException.retryAfterSeconds()`, …) and every
server error carries `traceId()` for Allegro support tickets. Retry with
equal-jitter backoff and lazy `Stream` pagination are on by default. SLF4J
debug channels (`…allegro.request` / `.auth` / `.retry`) trace every step the
SDK takes.

## Requirements

- Java 17+
- An Allegro application registered at the [developer portal](https://apps.developer.allegro.pl/)
  (client id / client secret)

## Coordinates

```
io.github.mgrtomaszzurawski:allegro-client:0.1.0-preview
```

## License

[GNU Affero General Public License v3.0 only](LICENSE.txt) — `AGPL-3.0-only`.
