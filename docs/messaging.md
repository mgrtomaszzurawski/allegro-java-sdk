# Messaging (Message Center)

The Allegro **message center** — buyer↔seller conversation threads, the messages within them,
and message attachments. Reached from the client via `messaging()`.

Backed by Allegro's `/messaging` resource. Every operation requires the `messaging` OAuth
scope. The facade works with **both a seller and a buyer** user-context token — the same SDK
serves both sides of a conversation.

```java
try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.PRODUCTION)) {
    Messaging messaging = client.messaging();
    // ...
}
```

## Browse threads and messages (lazy streams)

`streamThreads()` and `streamMessages(threadId)` return lazy `Stream`s: the SDK fetches one
page at a time and only requests the next page as you consume past the current one, so a
`limit(...)`/`findFirst()` touches the server minimally.

```java
List<MessageThread> recent = client.messaging().streamThreads().limit(20).toList();

MessageThread thread = client.messaging().thread(threadId);
List<Message> messages = client.messaging().streamMessages(threadId).toList();
```

A `MessageThread` is an immutable record: `id()`, `read()`, `lastMessageDateTime()` (may be
`null`) and `interlocutor()` (may be `null`). A `Message` carries `id()`, `status()`, `type()`,
`createdAt()`, `threadId()`, `author()`, `text()`, `subject()` (may be `null`), `relatesTo()`
(offer/order ids, either may be `null`), `hasAdditionalAttachments()`, `attachments()`, and
`vin()` (may be `null`). Enum-typed fields (`status`, `type`, attachment `status`) map to
`UNKNOWN` for any value a future server introduces that this release does not model yet.

To restrict a large thread to a time window, pass a `MessageFilter`:

```java
MessageFilter lastWeek = MessageFilter.builder()
        .after(OffsetDateTime.now().minusDays(7))
        .build();
List<Message> recent = client.messaging().streamMessages(threadId, lastWeek).toList();
```

## Mark a thread as read

```java
MessageThread updated = client.messaging().markRead(threadId);   // updated.read() == true
```

## Reply in a thread

Only the text is required; attachments are optional (see below).

```java
Message sent = client.messaging().reply(threadId,
        ReplyRequest.builder().text("Your parcel ships today.").build());
```

## Open a new thread

A new thread is opened by sending the first message to a recipient **in the context of an
order** — the Allegro contract requires the recipient login, an order id, and the text.

```java
Message sent = client.messaging().send(NewMessageRequest.builder()
        .recipientLogin(buyerLogin)
        .orderId(orderId)
        .text("Thanks for your order!")
        .build());
```

## Delete a message

```java
client.messaging().deleteMessage(messageId);
```

## Attachments

Attaching a file is a three-step flow: **declare** its name and byte size, **upload** the
bytes, then **reference** the returned id when composing a message. The server accepts
`image/png`, `image/gif`, `image/bmp`, `image/tiff`, `image/jpeg` and `application/pdf`, up to
5 MiB.

```java
byte[] invoice = Files.readAllBytes(path);

AttachmentRef declared = client.messaging().declareAttachment(AttachmentDeclaration.builder()
        .filename("invoice.pdf")
        .size(invoice.length)
        .build());
client.messaging().uploadAttachment(declared.id(), invoice, "application/pdf");

client.messaging().reply(threadId, ReplyRequest.builder()
        .text("Invoice attached.")
        .attachment(declared.id())
        .build());
```

Download an attachment's raw bytes with `downloadAttachment(attachmentId)`. An attachment is
only safe to download once its scan `status()` is `SAFE`.

## Errors

Failures are typed by remediation (see [`ARCHITECTURE.md`](../ARCHITECTURE.md)):

- `AllegroBadRequestException` — the message or attachment was rejected; `errors()` carries the
  field-level details (`code`, `path`, message).
- `AllegroNotFoundException` — no thread, message, or attachment with that id.
- `AllegroRateLimitException` — throttled; `retryAfterSeconds()` says how long to wait.
