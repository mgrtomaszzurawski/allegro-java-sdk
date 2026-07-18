/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.OfferRating;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.OfferTranslation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.OfferFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferSummary;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Live sandbox probe for bucket F — the read-only offer add-ons (rating,
 * translations, and bundles). Authenticates with the stored seller <em>user</em> token
 * (device-flow refresh token from the shared store, ADR-008), discovers the
 * seller's first offer via {@code streamOffers} (the demo runner forwards no
 * offer id), then reads that offer's rating and translations through the SDK.
 * Read-only — it does not modify any offer. Output is status-level only.
 *
 * <pre>
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=offer-extras -Pdemo.account=seller
 * </pre>
 */
public final class OfferExtrasDemo {

    /** Demo dispatch key (registered in {@code DemoApp}). */
    public static final String SCENARIO = "offer-extras";

    private static final String ERR_NO_STORED_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first";
    private static final String MSG_TOKEN_EXPIRED = "(stored token expired - rerun auth-bootstrap)";

    private OfferExtrasDemo() {
    }

    static void run(String clientId, String clientSecret, String account) throws IOException {
        SharedTokenStore tokenStore = new SharedTokenStore();
        String storedRefreshToken = tokenStore.load(account);
        if (storedRefreshToken == null) {
            System.out.println(ERR_NO_STORED_TOKEN.formatted(account));
            return;
        }
        DeviceCodeCredentials credentials = DeviceCodeCredentials.ofRefreshToken(
                clientId, clientSecret,
                ignored -> System.out.println(MSG_TOKEN_EXPIRED),
                storedRefreshToken);
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            Optional<OfferSummary> firstOffer = client.offers().streamOffers(OfferFilter.all()).findFirst();
            persistRotatedToken(client, tokenStore, account);
            if (firstOffer.isEmpty()) {
                System.out.println("(no offers on the account - cannot read rating/translations)");
                return;
            }
            String offerId = firstOffer.get().id();
            System.out.println("using offer " + offerId);

            OfferRating rating = client.offers().rating(offerId);
            System.out.println("offers.rating(" + offerId + "): average=" + rating.averageScore()
                    + " responses=" + rating.totalResponses());

            List<OfferTranslation> translations = client.offers().translations().ofOffer(offerId);
            System.out.println("offers.translations().ofOffer(" + offerId + "): "
                    + translations.size() + " language(s)");

            long bundleCount = client.offers().bundles().streamBundles().count();
            System.out.println("offers.bundles().streamBundles(): " + bundleCount + " bundle(s)");
        }
    }

    private static void persistRotatedToken(AllegroClient client, SharedTokenStore tokenStore, String account)
            throws IOException {
        // Allegro rotates the refresh token on every use — persist the new one
        // so the next run (or a sibling agent) keeps a valid session.
        String rotatedRefreshToken = client.refreshToken();
        if (rotatedRefreshToken != null) {
            tokenStore.store(account, rotatedRefreshToken);
        }
    }
}
