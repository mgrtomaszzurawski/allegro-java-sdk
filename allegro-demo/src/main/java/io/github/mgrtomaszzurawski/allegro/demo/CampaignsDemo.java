/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.AlleDiscount;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.AllegroPrices;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.Badges;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.AllegroPricesOfferQuery;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.BadgeApplicationFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.BadgeFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.EligibleOffersFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AlleDiscountCampaign;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AlleDiscountEligibleOffer;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AllegroPricesOfferStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AllegroPricesParticipation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.Badge;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.BadgeApplication;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.BadgeCampaign;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.MarketplaceParticipation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.OfferFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferSummary;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroFieldError;
import java.io.IOException;
import java.util.List;

/**
 * Sandbox read-shape probe for bucket H (campaigns), run via
 * {@code ./gradlew :allegro-demo:run -Pdemo.scenario=campaigns -Pdemo.account=seller}.
 *
 * <p>Reads badge campaigns through the SDK against the live sandbox and asserts
 * the response deserializes and maps — the read-only verification prescribed by
 * TESTING.md §2 for a discovery endpoint (there is nothing to create for it).
 * The write→read cycles for the applying/subsidy commands ship with the full
 * bucket. Output is status-level only — campaign metadata, never bodies or tokens.
 */
final class CampaignsDemo {

    private static final String NO_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first";
    private static final String STALE_TOKEN = "(stored token expired - rerun auth-bootstrap)";
    private static final String MARKETPLACE_PL = "allegro-pl";
    private static final long READ_SHAPE_SAMPLE = 5L;

    private CampaignsDemo() {
    }

    static void run(String clientId, String clientSecret, String account) throws IOException {
        SharedTokenStore tokenStore = new SharedTokenStore();
        String storedRefreshToken = tokenStore.load(account);
        if (storedRefreshToken == null) {
            System.out.println(NO_TOKEN.formatted(account));
            return;
        }
        DeviceCodeCredentials credentials = DeviceCodeCredentials.ofRefreshToken(
                clientId, clientSecret,
                ignored -> System.out.println(STALE_TOKEN),
                storedRefreshToken);
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            List<BadgeCampaign> campaigns = client.campaigns().badges().availableCampaigns();
            String rotatedRefreshToken = client.refreshToken();
            if (rotatedRefreshToken != null) {
                tokenStore.store(account, rotatedRefreshToken);
            }
            System.out.println("badges().availableCampaigns(): " + campaigns.size() + " campaign(s)");
            for (BadgeCampaign campaign : campaigns) {
                System.out.println("  - " + campaign.id() + " [" + campaign.type() + "] eligible="
                        + campaign.eligible());
            }
            probe("badges() read-shape",
                    () -> readShapeApplicationsAndBadges(client.campaigns().badges()));
            probe("allegroPrices() read-shape", () -> readShapeAllegroPrices(client));
            probe("alleDiscount() read-shape",
                    () -> readShapeAlleDiscount(client.campaigns().alleDiscount()));
        }
    }

    /**
     * Runs one read-shape probe, reporting a server rejection (with the parsed
     * {@code errors[]} field detail and trace id) instead of aborting the whole
     * scenario — so a quirk on one programme still lets the others be verified and
     * the rejection reason is captured for {@code KNOWN-SERVER-BEHAVIORS.md}.
     */
    private static void probe(String label, Runnable body) {
        try {
            body.run();
        } catch (AllegroBadRequestException badRequest) {
            System.out.println(label + " → 400 Bad Request (traceId=" + badRequest.traceId() + "):");
            for (AllegroFieldError error : badRequest.errors()) {
                System.out.println("    [" + error.code() + "] path=" + error.path()
                        + " message=" + error.message());
            }
        } catch (AllegroException failure) {
            System.out.println(label + " → " + failure.getClass().getSimpleName()
                    + " (traceId=" + failure.traceId() + "): " + failure.getMessage());
        }
    }

    /**
     * Read-shape check of AlleDiscount through the SDK — the campaign list and, for
     * the first campaign, a sample of its eligible offers.
     */
    private static void readShapeAlleDiscount(AlleDiscount alleDiscount) {
        List<AlleDiscountCampaign> campaigns = alleDiscount.campaigns();
        System.out.println("alleDiscount().campaigns(): " + campaigns.size() + " campaign(s)");
        for (AlleDiscountCampaign campaign : campaigns) {
            System.out.println("  - " + campaign.id() + " [" + campaign.type() + "] " + campaign.name());
        }
        if (!campaigns.isEmpty()) {
            String campaignId = campaigns.get(0).id();
            List<AlleDiscountEligibleOffer> eligible = alleDiscount
                    .streamEligibleOffers(EligibleOffersFilter.builder(campaignId).build())
                    .limit(READ_SHAPE_SAMPLE)
                    .toList();
            System.out.println("alleDiscount().streamEligibleOffers(" + campaignId + "): "
                    + eligible.size() + " sampled");
            for (AlleDiscountEligibleOffer offer : eligible) {
                System.out.println("  - " + offer.offerId() + " requiredMerchantPrice="
                        + offer.requiredMerchantPrice() + " meetsConditions=" + offer.meetsConditions());
            }
        }
    }

    /**
     * Read-shape check of Allegro Prices through the SDK — participation across
     * marketplaces and a sample of per-offer status. The offer-status query
     * exercises the typed {@code oneOf} price-reduction mapping (resolved by the
     * strict {@code oneOf} mapper) on a live response; the server requires
     * {@code offer.ids} of size 1..1000 (see {@code KNOWN-SERVER-BEHAVIORS.md}), so
     * the probe first sources a few of the seller's own offer ids and skips with a
     * note when the account has no offers to query.
     */
    private static void readShapeAllegroPrices(AllegroClient client) {
        AllegroPrices allegroPrices = client.campaigns().allegroPrices();
        AllegroPricesParticipation participation = allegroPrices.participation();
        System.out.println("allegroPrices().participation(): "
                + participation.marketplaces().size() + " marketplace(s)");
        for (MarketplaceParticipation marketplace : participation.marketplaces()) {
            System.out.println("  - " + marketplace.marketplaceId() + " → " + marketplace.status());
        }
        List<String> offerIds = client.offers().streamOffers(OfferFilter.all())
                .limit(READ_SHAPE_SAMPLE)
                .map(OfferSummary::id)
                .toList();
        if (offerIds.isEmpty()) {
            System.out.println("allegroPrices().streamOffersStatus: skipped (seller has no offers to query)");
            return;
        }
        AllegroPricesOfferQuery.Builder query = AllegroPricesOfferQuery.builder(MARKETPLACE_PL);
        offerIds.forEach(query::addOfferId);
        List<AllegroPricesOfferStatus> statuses = allegroPrices
                .streamOffersStatus(query.build())
                .limit(READ_SHAPE_SAMPLE)
                .toList();
        System.out.println("allegroPrices().streamOffersStatus(" + offerIds.size() + " offer id(s)): "
                + statuses.size() + " sampled");
        for (AllegroPricesOfferStatus status : statuses) {
            System.out.println("  - " + status.offerId() + " base=" + status.basePrice()
                    + " opportunity=" + status.discountOpportunity()
                    + " actualReduction=" + status.actualReductionPercentage());
        }
    }

    /**
     * Read-shape check of the seller's badge applications and active badges through
     * the SDK — a sample is streamed so every mapped field is proven to arrive
     * parseable on a live response (TESTING.md §2). Bounded to a handful of items.
     */
    private static void readShapeApplicationsAndBadges(Badges badges) {
        List<BadgeApplication> applications = badges.streamApplications(BadgeApplicationFilter.all())
                .limit(READ_SHAPE_SAMPLE)
                .toList();
        System.out.println("badges().streamApplications(): " + applications.size() + " sampled");
        for (BadgeApplication application : applications) {
            System.out.println("  - " + application.id() + " " + application.campaignId() + " → "
                    + application.status());
        }
        List<Badge> activeBadges = badges.streamBadges(BadgeFilter.builder()
                        .marketplaceId(MARKETPLACE_PL).build())
                .limit(READ_SHAPE_SAMPLE)
                .toList();
        System.out.println("badges().streamBadges(" + MARKETPLACE_PL + "): "
                + activeBadges.size() + " sampled");
        for (Badge badge : activeBadges) {
            System.out.println("  - " + badge.offerId() + " " + badge.campaignId() + " → "
                    + badge.status());
        }
    }
}
