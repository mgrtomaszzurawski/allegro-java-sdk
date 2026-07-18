/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.TagRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.Tag;
import java.io.IOException;
import java.util.List;

/**
 * Live sandbox probe for bucket F — offer tags. Runs the write→read cycle through
 * the SDK with the stored seller <em>user</em> token (device-flow refresh token
 * from the shared store, ADR-008): create a tag, find it in the streamed tag
 * list, then delete it (cleanup). When an advertisement {@code -Pdemo.offerId} is
 * supplied it also assigns the tag to the offer and reads it back before delete.
 * Output is status-level only — never bodies or tokens.
 *
 * <pre>
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=offer-tags -Pdemo.account=seller \
 *       [-Pdemo.offerId=&lt;offerId&gt;]
 * </pre>
 */
public final class OfferTagsDemo {

    /** Demo dispatch key (registered in {@code DemoApp}). */
    public static final String SCENARIO = "offer-tags";

    private static final String OFFER_ID_PROPERTY = "demo.offerId";
    private static final String DEMO_TAG_NAME = "[F-demo] tags-probe";
    private static final String ERR_NO_STORED_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first";
    private static final String MSG_TOKEN_EXPIRED = "(stored token expired - rerun auth-bootstrap)";

    private OfferTagsDemo() {
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
            String tagId = client.offers().tags()
                    .create(TagRequest.builder().name(DEMO_TAG_NAME).build());
            persistRotatedToken(client, tokenStore, account);
            System.out.println("tags.create(" + DEMO_TAG_NAME + "): id=" + tagId);

            boolean found = client.offers().tags().streamTags()
                    .anyMatch(tag -> tagId.equals(tag.id()));
            System.out.println("tags.streamTags(): created tag present=" + found);

            assignCycle(client, tagId);

            client.offers().tags().delete(tagId);
            System.out.println("tags.delete(" + tagId + "): done");
        }
    }

    private static void assignCycle(AllegroClient client, String tagId) {
        String offerId = System.getProperty(OFFER_ID_PROPERTY);
        if (offerId == null || offerId.isBlank()) {
            System.out.println("(no -Pdemo.offerId - skipping assign write→read)");
            return;
        }
        client.offers().tags().assignToOffer(offerId, List.of(tagId));
        List<Tag> assigned = client.offers().tags().ofOffer(offerId);
        boolean roundTrips = assigned.stream().anyMatch(tag -> tagId.equals(tag.id()));
        System.out.println("tags.assignToOffer/ofOffer(" + offerId + "): roundTrips=" + roundTrips);
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
