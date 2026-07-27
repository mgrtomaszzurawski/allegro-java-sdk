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
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroFieldError;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private static final String BUNDLE_OFFER_IDS_PROPERTY = "demo.bundleOfferIds";
    private static final String OFFER_ID_SEPARATOR = ",";
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
            List<String> offerIds = resolveOfferIds(client);
            persistRotatedToken(client, tokenStore, account);
            if (offerIds.size() < OFFERS_NEEDED) {
                System.out.println("(need at least " + OFFERS_NEEDED
                        + " offers of distinct products to build a flexible bundle)");
                return;
            }
            System.out.println("bundling offers " + offerIds);
            createReadDelete(client.offers().flexibleBundles(), offerIds);
            persistRotatedToken(client, tokenStore, account);
        }
    }

    /**
     * Explicit {@code -Pdemo.bundleOfferIds=a,b} wins (deterministic, lets the
     * operator pick offers of known-distinct products); otherwise auto-discover,
     * de-duplicating by offer name so two listings of the same product are not
     * picked — the server rejects a same-product pair with
     * {@code OffersRelatedToOneProductException}.
     */
    private static List<String> resolveOfferIds(AllegroClient client) {
        String override = System.getProperty(BUNDLE_OFFER_IDS_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Arrays.stream(override.split(OFFER_ID_SEPARATOR))
                    .map(String::trim)
                    .filter(candidate -> !candidate.isEmpty())
                    .toList();
        }
        Set<String> seenNames = new HashSet<>();
        return client.offers().streamOffers(OfferFilter.all())
                .filter(summary -> seenNames.add(summary.name()))
                .limit(OFFERS_NEEDED)
                .map(OfferSummary::id)
                .toList();
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
            readBack.slots().forEach(slot -> slot.offers().forEach(bundleOffer ->
                    System.out.println("  slot offer " + bundleOffer.offerId()
                            + " marketplaces=" + bundleOffer.marketplaces())));
            flexible.delete(created.id());
            System.out.println("deleted flexible bundle " + created.id());
        } catch (AllegroBadRequestException rejected) {
            System.out.println("create/read/delete rejected as invalid (traceId="
                    + rejected.traceId() + "):");
            for (AllegroFieldError error : rejected.errors()) {
                System.out.println("  - [" + error.code() + "] path=" + error.path()
                        + " userMessage=" + error.userMessage());
            }
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
