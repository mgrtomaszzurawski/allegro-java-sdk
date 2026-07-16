# Allegro Java SDK

A typed, hand-crafted Java client library for the [Allegro REST API](https://developer.allegro.pl/documentation/).

> **Status: preview (`0.1.0-preview`).** Under active development. The public API is not yet stable.

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
