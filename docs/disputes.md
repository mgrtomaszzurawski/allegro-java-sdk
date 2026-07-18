# Disputes (Post-Purchase Issues)

Post-purchase **issues** — disputes and claims a buyer raises against an order. Reached from
the client via `disputes()`.

Backed by Allegro's `/sale/issues` resource, a **beta** API served with the
`application/vnd.allegro.beta.v1+json` media type (the SDK sets it on both the `Accept` and the
request-body `Content-Type` for you). Every operation requires the `disputes` OAuth scope.
Issues are opened by buyers: read the issues on the account, read one, read its chat, then
respond as the seller — post a message, change a claim's status, or attach a file.

```java
try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.PRODUCTION)) {
    Disputes disputes = client.disputes();
    // ...
}
```

## Browse issues (lazy stream)

`streamIssues(IssueFilter)` returns a lazy `Stream` — the SDK fetches one page at a time and
only requests the next as you consume past the current one. Filter by status (any-of) and/or
the order the issue concerns:

```java
IssueFilter filter = IssueFilter.builder()
        .status(IssueStatus.DISPUTE_ONGOING)
        .status(IssueStatus.CLAIM_SUBMITTED)   // any-of
        .checkoutFormId(orderId)               // optional
        .build();

List<Issue> issues = client.disputes().streamIssues(filter).limit(50).toList();
```

Use `IssueFilter.none()` to stream every issue.

An `Issue` is an immutable record: `id()`, `type()` (`DISPUTE`/`CLAIM`), `right()`
(`WARRANTY`/`COMPLAINT`), `referenceNumber()`, `subject()`, `description()`, `openedDate()`,
`decisionDueDate()`, `buyer()`, `checkoutFormId()`, and `state()` (status + due date +
`returnRequired` + `chatActive`). Enum-typed fields fall back to `UNKNOWN` for any value a
future server introduces. The issues surface is beta and marks no field as required, so most
getters are nullable.

## Read one issue

```java
Issue issue = client.disputes().get(issueId);
```

## Read the chat

`streamChat(issueId)` streams the issue's conversation — messages and state changes — oldest
first:

```java
client.disputes().streamChat(issueId).forEach(entry ->
        System.out.println(entry.author().role() + ": " + entry.text()));
```

An `IssueChatEntry` carries `id()`, `text()`, `createdAt()`, `author()` (login + `role()`:
`BUYER`/`SELLER`/`ADMIN`/`SYSTEM`/`FULFILLMENT`), and `attachments()` (file name + URL).

## Post a message

Add a seller message to an issue. A message must carry text, at least one attachment, or both;
the type defaults to `REGULAR`:

```java
IssueMessageRequest reply = IssueMessageRequest.builder()
        .text("Thank you — a replacement ships today.")
        .build();

IssueChatEntry added = client.disputes().addMessage(issueId, reply);
```

A non-`REGULAR` type drives a formal transition: `END_REQUEST` ends a dispute, while the
`RETURN_*` types answer a return on a claim. Allegro restricts each type to the right context
(dispute vs. claim).

## Change a claim's status

Accept a claim (optionally with a partial refund) or reject it with a documented reason. Valid
for claims only, never for disputes:

```java
ClaimStatusChange decision = ClaimStatusChange.builder()
        .status(ClaimStatus.ACCEPTED_PARTIAL_REFUND)
        .message("We agree to a partial refund.")
        .partialRefund(Money.of("12.50", "PLN"))   // only for ACCEPTED_PARTIAL_REFUND
        .build();

client.disputes().changeStatus(issueId, decision);
```

## Attach a file to a message

Upload the file, then reference the returned id when posting a message. The SDK declares the
attachment and streams the bytes to the one-time upload location Allegro returns — you never
handle that URL:

```java
byte[] bytes = Files.readAllBytes(path);
IssueAttachmentDeclaration declaration = IssueAttachmentDeclaration.builder()
        .filename("evidence.jpg")
        .size(bytes.length)          // must equal the byte count
        .build();

IssueAttachmentRef attachment = client.disputes()
        .uploadAttachment(declaration, bytes, "image/jpeg");

client.disputes().addMessage(issueId, IssueMessageRequest.builder()
        .text("Please see the attached photo.")
        .attachment(attachment.id())
        .build());
```

Read an attachment's bytes back with `downloadAttachment(attachmentId)`.

## Errors

Failures are typed by remediation (see [`ARCHITECTURE.md`](../ARCHITECTURE.md)):

- `AllegroBadRequestException` — the request was rejected; `errors()` carries the field-level
  details (`code`, `path`, message).
- `AllegroNotFoundException` — no issue with that id.
- `AllegroRateLimitException` — throttled; `retryAfterSeconds()` says how long to wait.
