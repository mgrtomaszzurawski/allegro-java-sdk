# Campaigns

Marketing-campaign programmes, reached from `client.campaigns()` (bucket H). Every operation
needs a **seller user-context token** with the `allegro:api:campaigns` scope — an app-only
client-credentials token cannot see a seller's campaigns.

The bucket covers three programmes: **badge campaigns**, **Allegro Prices**, and **AlleDiscount** —
all complete.

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

## Allegro Prices

`client.campaigns().allegroPrices()` manages the seller's participation in the Allegro Prices
subsidy programme, reads per-offer status, and submits or excludes offers.

### Participation

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.ParticipationUpdate;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AllegroPricesParticipation;

AllegroPricesParticipation current = client.campaigns().allegroPrices().participation();

// opt in on allegro-pl, out on allegro-cz
AllegroPricesParticipation updated = client.campaigns().allegroPrices().updateParticipation(
        ParticipationUpdate.builder().allow("allegro-pl").deny("allegro-cz").build());
```

Each `MarketplaceParticipation` carries the `marketplaceId` and a `ParticipationStatus`
(`ALLOWED` / `DENIED`).

### Offer status

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.AllegroPricesOfferQuery;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.OfferScope;

client.campaigns().allegroPrices()
        .streamOffersStatus(AllegroPricesOfferQuery.builder("allegro-pl")
                .addOfferId("12345678")
                .scope(OfferScope.DISCOUNTED)
                .build())
        .forEach(status -> System.out.println(status.offerId() + " → " + status.finalBuyerPrice()));
```

A lazy `Stream<AllegroPricesOfferStatus>`: each item exposes the base price, whether Allegro sees a
discount opportunity, the recommended / declared / actual subsidy reduction percentages, and the
resulting buyer price. `marketplaceId` is required; the query is a POST whose pagination is handled
internally. Allegro also requires at least one offer id — add one or more with `addOfferId(...)`; a
marketplace-only query is rejected with `VALIDATION_ERROR` (`offer.ids size must be between 1 and
1,000`), see `KNOWN-SERVER-BEHAVIORS.md`.

### Submitting and excluding offers

`submitOffers` and `excludeOffers` are subsidy commands: the call blocks until every offer reaches a
terminal state and returns a per-offer `SubsidyCommandReport`.

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.SubmitOffersRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.SubsidyCommandReport;

SubsidyCommandReport report = client.campaigns().allegroPrices().submitOffers(
        SubmitOffersRequest.builder()
                .addOffer("12345678", "allegro-pl", "5")   // max seller contribution %
                .build());

report.offers().forEach(offer ->
        System.out.println(offer.offerId() + " → " + offer.status() + " " + offer.errors()));
```

Each `SubsidyOfferResult` also carries `maxContributionPercentage()` — the max seller contribution
Allegro recorded for that offer, echoed back from the submit declaration, so you can confirm the
accepted value without a second read. It is `null` on an `excludeOffers` result (an exclusion
declares no contribution).

> **Alpha.** `maxContributionPercentage()` is mapped from the vendored OpenAPI spec but is **not
> yet confirmed against the live sandbox** — a subsidy submit needs an offer with a discount
> opportunity, which the current sandbox account cannot arrange. Verify it manually before relying
> on it, or treat it as alpha. Every other field on the report is live-verified.

Between 1 and 1000 offers per command. A `Duration` overload bounds the wait; on expiry the SDK
throws `AllegroAsyncTimeoutException` (the command may still complete server-side — re-read the
offer status rather than resubmitting). `excludeOffers` works the same way with
`ExcludeOffersRequest`.

## AlleDiscount

`client.campaigns().alleDiscount()` covers the AlleDiscount programme: discover campaigns, read
which offers are eligible or already submitted, and submit or withdraw an offer.

```java
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.EligibleOffersFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.SubmitOfferRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AlleDiscountCampaign;

for (AlleDiscountCampaign campaign : client.campaigns().alleDiscount().campaigns()) {
    // eligible offers for this campaign (lazy Stream)
    client.campaigns().alleDiscount()
            .streamEligibleOffers(EligibleOffersFilter.builder(campaign.id())
                    .meetsConditions(true)
                    .build())
            .forEach(offer -> System.out.println(
                    offer.offerId() + " ≤ " + offer.requiredMerchantPrice()));
}
```

`campaigns()` returns a `List` (the endpoint is not paginated); `streamEligibleOffers` and
`streamSubmittedOffers` are lazy `Stream`s. Each eligible offer exposes the `requiredMerchantPrice`
(the price ceiling for participation), the minimum guaranteed discount, and whether it
`meetsConditions()` (with `conditionViolations()` explaining any gap).

Submitting and withdrawing are commands — the call blocks until the command reaches a terminal
state:

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AlleDiscountSubmitResult;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AlleDiscountWithdrawResult;

AlleDiscountSubmitResult submitted = client.campaigns().alleDiscount().submitOffer(
        SubmitOfferRequest.builder()
                .campaignId("winter-sale")
                .offerId("12345678")
                .proposedPrice(Money.of("24.99", "PLN"))   // must not exceed requiredMerchantPrice
                .build());
// submitted.status() is SUCCESSFUL or FAILED; submitted.participationId() on success

AlleDiscountWithdrawResult withdrawn =
        client.campaigns().alleDiscount().withdrawOffer(submitted.participationId());
```

Withdrawal keys on the `participationId` (from a submitted offer or a successful submit result).
Both commands take an optional `Duration` timeout; on expiry the SDK throws
`AllegroAsyncTimeoutException` (the command may still complete server-side).

## Errors

Campaign calls surface the SDK's standard remediation-grouped exceptions — a missing resource is an
`AllegroNotFoundException`, an expired token is re-authenticated and replayed once automatically,
and every exception carries the server `traceId()` for support tickets. See the exception hierarchy
in [`ARCHITECTURE.md`](../ARCHITECTURE.md).
