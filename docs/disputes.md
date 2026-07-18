# Disputes (Post-Purchase Issues)

Post-purchase **issues** — disputes and claims a buyer raises against an order. Reached from
the client via `disputes()`.

Backed by Allegro's `/sale/issues` resource, a **beta** API served with the
`application/vnd.allegro.beta.v1+json` media type (the SDK sets it for you). Every operation
requires the `disputes` OAuth scope. Issues are opened by buyers, so this facade is
**read-oriented**: list the issues on the account, read one, and read its chat.

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

## Responding to a dispute

The seller-side writes of this resource — add a message, change a claim's status, attach a
file — need a beta request-body media type that the shared transport does not yet expose. They
ship in a follow-up once that core capability lands.

## Errors

Failures are typed by remediation (see [`ARCHITECTURE.md`](../ARCHITECTURE.md)):

- `AllegroBadRequestException` — the request was rejected; `errors()` carries the field-level
  details (`code`, `path`, message).
- `AllegroNotFoundException` — no issue with that id.
- `AllegroRateLimitException` — throttled; `retryAfterSeconds()` says how long to wait.
