# Contacts

Seller **contact cards** — reusable blocks of contact data (a display name, an optional
e-mail, up to two phone numbers) that offers can reference, for example on classified
advertisements. Reached from the client via `contacts()`.

Backed by Allegro's `/sale/offer-contacts` resource. Reads require the `sale:settings:read`
scope; writes require `sale:settings:write`.

```java
try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.PRODUCTION)) {
    Contacts contacts = client.contacts();
    // ...
}
```

## List all contact cards

The resource is not paginated — the full set comes back in one call.

```java
List<Contact> cards = client.contacts().list();
for (Contact card : cards) {
    System.out.println(card.id() + " → " + card.name());
}
```

## Read one card

```java
Contact card = client.contacts().get(contactId);
```

A `Contact` is an immutable record: `id()`, `name()` (may be `null`), `emails()` and
`phones()` (never `null`, possibly empty). The wire wraps each e-mail and phone in a
single-field object; the SDK flattens them to plain strings.

## Create a card

Requests are built with a fluent, fail-fast builder. The Allegro contract marks no field as
required, but it does limit sizes — the builder rejects an over-long name (> 250 characters),
e-mail (> 128), or phone number (> 250), and refuses a second e-mail or a third phone number.

```java
ContactRequest request = ContactRequest.builder()
        .name("Main contact")
        .email("shop@example.com")   // at most one
        .phone("+48512323495")       // up to two
        .build();

Contact created = client.contacts().create(request);
```

## Update a card

An update **replaces the whole card**, so carry over any channels you want to keep:

```java
Contact current = client.contacts().get(contactId);
ContactRequest.Builder request = ContactRequest.builder().name("Returns contact");
current.emails().forEach(request::email);
current.phones().forEach(request::phone);

Contact updated = client.contacts().update(contactId, request.build());
```

`toBuilder()` gives you a pre-filled builder when you already hold a `ContactRequest`.

## Errors

Failures are typed by remediation (see [`ARCHITECTURE.md`](../ARCHITECTURE.md)):

- `AllegroBadRequestException` — validation rejected the card; `errors()` carries the
  field-level details (`code`, `path`, message).
- `AllegroNotFoundException` — no card with that id.
- `AllegroRateLimitException` — throttled; `retryAfterSeconds()` says how long to wait.

There is no delete operation on this resource.
