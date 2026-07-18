# Campaigns

Marketing-campaign programmes, reached from `client.campaigns()` (bucket H). Every operation
needs a **seller user-context token** with the `allegro:api:campaigns` scope — an app-only
client-credentials token cannot see a seller's campaigns.

The bucket covers three programmes: **badge campaigns**, **Allegro Prices**, and **AlleDiscount**.
The badge-campaigns programme is complete; Allegro Prices and AlleDiscount land next.

## Badge campaigns

`client.campaigns().badges()` groups the badge-campaign operations: discover campaigns, apply an
offer for a badge, track applications and active badges, and update or finish a badge. Discovery is
the entry point:

```java
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.BadgeCampaign;

try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.PRODUCTION)) {
    for (BadgeCampaign campaign : client.campaigns().badges().availableCampaigns()) {
        System.out.println(campaign.id() + " — " + campaign.name()
                + (campaign.eligible() ? " (eligible)" : " (not eligible)"));
    }
}
```

`availableCampaigns()` returns every campaign offered to the authenticated seller across all
marketplaces; the `availableCampaigns(String marketplaceId)` overload restricts the result to one
marketplace (for example `"allegro-pl"`).

Each [`BadgeCampaign`](../allegro-client/src/main/java/io/github/mgrtomaszzurawski/allegro/sdk/domain/campaigns/model/BadgeCampaign.java)
carries the seller's own **eligibility** for the campaign and, when not eligible, the
`refusalReasons()` explaining why — so a caller can decide whether applying is worthwhile before
building a request. It also exposes the campaign's three time windows:

| Window | Meaning |
|---|---|
| `application()` | when offers may be submitted to the campaign |
| `visibility()`  | when the campaign is visible in the seller's tools |
| `publication()` | when the badge is displayed to buyers |

Each window is a `CampaignSchedule` whose `type()` says how it is bounded (`ALWAYS`, `SINCE`,
`WITHIN`, `UNTIL`, `NEVER`); `startsAt()`/`endsAt()` are present only when the policy uses them.

### Applying an offer for a badge

Build a request with the fluent builder — `campaignId` and `offerId` are required; the bargain
price, per-buyer limit and declared stock are optional and depend on the campaign's rules:

```java
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.BadgeApplicationRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.BadgeApplication;

BadgeApplicationRequest request = BadgeApplicationRequest.builder()
        .campaignId("BARGAIN")
        .offerId("12345678")
        .bargainPrice(Money.of("29.99", "PLN"))
        .build();

BadgeApplication application = client.campaigns().badges().apply(request);
// application.status() == BadgeApplicationStatus.REQUESTED
```

Allegro verifies a badge application asynchronously — often with a human, e-mail-notified step that
can take a long time — so `apply(...)` does **not** block waiting for a verdict. It returns the
created application straight away (usually `REQUESTED`); you track the outcome later with
`application(id)` or `streamApplications(...)`. (This is a deliberate, spec-grounded exception to
the SDK's usual "wrap async commands synchronously" rule (ADR-005), which fits the short badge
`update` operation below but not an e-mail-verified review.)

### Tracking applications and active badges

```java
// applications the seller has submitted (lazy, paged)
client.campaigns().badges()
        .streamApplications(BadgeApplicationFilter.builder().offerId("12345678").build())
        .forEach(app -> System.out.println(app.campaignId() + " → " + app.status()));

// a single application by id
BadgeApplication one = client.campaigns().badges().application("app-1");

// badges currently active on the seller's offers, per marketplace (marketplace is required)
client.campaigns().badges()
        .streamBadges(BadgeFilter.builder().marketplaceId("allegro-pl").build())
        .forEach(badge -> System.out.println(badge.offerId() + " → " + badge.status()));
```

Both streams are lazy `Stream`s: pages are fetched from Allegro only as the stream is consumed.
A declined application or badge carries `rejectionReasons()` explaining why.

### Updating or finishing a badge

`update(...)` submits a short badge operation and, unlike `apply(...)`, blocks until it reaches a
terminal state (polling internally per ADR-005). A `BadgePatch` carries exactly one change:

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.BadgePatch;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.BadgeOperation;

// change the badge's bargain price
BadgeOperation changed = client.campaigns().badges()
        .update("12345678", "BARGAIN", BadgePatch.changeBargainPrice(Money.of("24.99", "PLN")));

// finish (end) the badge
BadgeOperation finished = client.campaigns().badges()
        .update("12345678", "BARGAIN", BadgePatch.finish());
// finished.status() is PROCESSED or DECLINED — the operation reached a terminal state
```

An overload takes a `Duration` timeout; if the operation does not finish within it, the SDK throws
`AllegroAsyncTimeoutException` (the operation may still complete server-side — re-read the badge
rather than resubmitting).

## Errors

Campaign calls surface the SDK's standard remediation-grouped exceptions — a missing resource is an
`AllegroNotFoundException`, an expired token is re-authenticated and replayed once automatically,
and every exception carries the server `traceId()` for support tickets. See the exception hierarchy
in [`ARCHITECTURE.md`](../ARCHITECTURE.md).
