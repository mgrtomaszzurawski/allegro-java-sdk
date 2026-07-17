# Campaigns

Marketing-campaign programmes, reached from `client.campaigns()` (bucket H). Every operation
needs a **seller user-context token** with the `allegro:api:campaigns` scope — an app-only
client-credentials token cannot see a seller's campaigns.

The bucket covers three programmes: **badge campaigns**, **Allegro Prices**, and **AlleDiscount**.
This starter slice ships the first read of the badge-campaigns programme; the rest lands with the
full bucket.

## Badge campaigns

`client.campaigns().badges()` groups the badge-campaign operations (apply an offer for a badge,
list applications and active badges, update a badge). The first available operation is discovery:

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

## Errors

Campaign calls surface the SDK's standard remediation-grouped exceptions — a missing resource is an
`AllegroNotFoundException`, an expired token is re-authenticated and replayed once automatically,
and every exception carries the server `traceId()` for support tickets. See the exception hierarchy
in [`ARCHITECTURE.md`](../ARCHITECTURE.md).
