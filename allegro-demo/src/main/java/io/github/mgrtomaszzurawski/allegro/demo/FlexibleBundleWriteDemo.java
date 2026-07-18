/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.FlexibleBundles;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.FlexibleBundleOfferRef;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.FlexibleBundleRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.FlexibleBundleSlotRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.FlexibleBundle;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.FlexibleBundleDiscount;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.MarketplaceDiscount;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.WholeBundleDiscount;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.OfferFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferSummary;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroException;
import java.io.IOException;
import java.util.List;

/**
 * Live sandbox probe for bucket F flexible-bundle <em>writes</em> — a
 * create&rarr;read&rarr;delete round-trip through the SDK. Authenticates with the
 * stored seller <em>user</em> token (ADR-008), discovers the account's first two
 * offers via {@code streamOffers}, builds a two-slot bundle with a whole-bundle
 * discount, creates it, reads it back, and deletes it. If the account has fewer
 * than two offers, or the server rejects the pairing (an offer not bundleable on a
 * marketplace), the reason is printed — server surprises go to
 * {@code KNOWN-SERVER-BEHAVIORS.md}.
 *
 * <pre>
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=flexible-bundle-write -Pdemo.account=seller
 * </pre>
 */
public final class FlexibleBundleWriteDemo {

    /** Demo dispatch key (registered in {@code DemoApp}). */
    public static final String SCENARIO = "flexible-bundle-write";

    private static final String ERR_NO_STORED_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first";
    private static final String MSG_TOKEN_EXPIRED = "(stored token expired - rerun auth-bootstrap)";
    private static final String MARKETPLACE_ID = "allegro-pl";
    private static final int OFFERS_NEEDED = 2;
    private static final int MINIMUM_BOUGHT_OFFERS = 2;
    private static final int DISCOUNT_PERCENTAGE = 5;
    private static final int FIRST_SLOT_ORDER = 0;
    private static final int SECOND_SLOT_ORDER = 1;
    private static final int REQUIRED_QUANTITY = 1;

    private FlexibleBundleWriteDemo() {
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
            List<String> offerIds = client.offers().streamOffers(OfferFilter.all())
                    .limit(OFFERS_NEEDED)
                    .map(OfferSummary::id)
                    .toList();
            persistRotatedToken(client, tokenStore, account);
            if (offerIds.size() < OFFERS_NEEDED) {
                System.out.println("(need at least " + OFFERS_NEEDED + " offers to build a flexible bundle)");
                return;
            }
            createReadDelete(client.offers().flexibleBundles(), offerIds);
            persistRotatedToken(client, tokenStore, account);
        }
    }

    private static void createReadDelete(FlexibleBundles flexible, List<String> offerIds) {
        FlexibleBundleRequest request = FlexibleBundleRequest.builder()
                .slot(FlexibleBundleSlotRequest.builder()
                        .order(FIRST_SLOT_ORDER).entryPoint(true).requiredQuantity(REQUIRED_QUANTITY)
                        .offer(FlexibleBundleOfferRef.of(offerIds.get(0), false))
                        .build())
                .slot(FlexibleBundleSlotRequest.builder()
                        .order(SECOND_SLOT_ORDER).requiredQuantity(REQUIRED_QUANTITY)
                        .offer(FlexibleBundleOfferRef.of(offerIds.get(1), false))
                        .build())
                .discount(FlexibleBundleDiscount.wholeBundle(new WholeBundleDiscount(
                        MINIMUM_BOUGHT_OFFERS, List.of(new MarketplaceDiscount(MARKETPLACE_ID, DISCOUNT_PERCENTAGE)))))
                .build();
        try {
            FlexibleBundle created = flexible.create(request);
            System.out.println("created flexible bundle " + created.id()
                    + " (" + created.slots().size() + " slots, createdBy=" + created.createdBy() + ")");
            FlexibleBundle readBack = flexible.get(created.id());
            System.out.println("read back bundle " + readBack.id() + " with " + readBack.slots().size() + " slot(s)");
            flexible.delete(created.id());
            System.out.println("deleted flexible bundle " + created.id());
        } catch (AllegroException failure) {
            System.out.println("create/read/delete failed: " + failure.getMessage()
                    + " (traceId=" + failure.traceId() + ")");
        }
    }

    private static void persistRotatedToken(AllegroClient client, SharedTokenStore tokenStore, String account)
            throws IOException {
        String rotatedRefreshToken = client.refreshToken();
        if (rotatedRefreshToken != null) {
            tokenStore.store(account, rotatedRefreshToken);
        }
    }
}
