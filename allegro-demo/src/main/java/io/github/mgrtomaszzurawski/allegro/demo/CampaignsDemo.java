/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.AllegroPrices;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.Badges;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.AllegroPricesOfferQuery;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.BadgeApplicationFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.BadgeFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AllegroPricesOfferStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AllegroPricesParticipation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.Badge;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.BadgeApplication;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.BadgeCampaign;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.MarketplaceParticipation;
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
            readShapeApplicationsAndBadges(client.campaigns().badges());
            readShapeAllegroPrices(client.campaigns().allegroPrices());
        }
    }

    /**
     * Read-shape check of Allegro Prices through the SDK — participation across
     * marketplaces and a sample of per-offer status (which exercises the raw-JSON
     * {@code oneOf} price-reduction mapping on a live response).
     */
    private static void readShapeAllegroPrices(AllegroPrices allegroPrices) {
        AllegroPricesParticipation participation = allegroPrices.participation();
        System.out.println("allegroPrices().participation(): "
                + participation.marketplaces().size() + " marketplace(s)");
        for (MarketplaceParticipation marketplace : participation.marketplaces()) {
            System.out.println("  - " + marketplace.marketplaceId() + " → " + marketplace.status());
        }
        List<AllegroPricesOfferStatus> statuses = allegroPrices
                .streamOffersStatus(AllegroPricesOfferQuery.builder(MARKETPLACE_PL).build())
                .limit(READ_SHAPE_SAMPLE)
                .toList();
        System.out.println("allegroPrices().streamOffersStatus(" + MARKETPLACE_PL + "): "
                + statuses.size() + " sampled");
        for (AllegroPricesOfferStatus status : statuses) {
            System.out.println("  - " + status.offerId() + " base=" + status.basePrice()
                    + " opportunity=" + status.discountOpportunity());
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
