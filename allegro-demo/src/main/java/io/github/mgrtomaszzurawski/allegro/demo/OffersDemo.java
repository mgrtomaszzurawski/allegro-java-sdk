/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.Offer;
import java.io.IOException;

/**
 * Bucket A sandbox probe: read an offer, and — when {@code -Pdemo.offerId} and
 * {@code -Pdemo.newPrice} are given — run the write→read cycle (change the Buy
 * Now price through the SDK, then read it back and confirm the round-trip).
 *
 * <pre>
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=offer -Pdemo.offerId=13579
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=offer -Pdemo.offerId=13579 -Pdemo.newPrice=149.50
 * </pre>
 *
 * Needs a seller offer that already exists on the sandbox account (offer create
 * lands in the bucket's main package); pass its id via {@code demo.offerId}.
 */
final class OffersDemo {

    private static final String OFFER_ID_PROPERTY = "demo.offerId";
    private static final String NEW_PRICE_PROPERTY = "demo.newPrice";
    private static final String CURRENCY_PLN = "PLN";
    private static final String ERR_NO_OFFER_ID =
            "Pass -Pdemo.offerId=<id> (a seller offer that exists on the sandbox account)";
    private static final String ERR_NO_STORED_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first";

    private OffersDemo() {
    }

    static void run(String clientId, String clientSecret, String account) throws IOException {
        String offerId = System.getProperty(OFFER_ID_PROPERTY);
        if (offerId == null) {
            System.out.println(ERR_NO_OFFER_ID);
            return;
        }
        SharedTokenStore tokenStore = new SharedTokenStore();
        String storedRefreshToken = tokenStore.load(account);
        if (storedRefreshToken == null) {
            System.out.println(ERR_NO_STORED_TOKEN.formatted(account));
            return;
        }
        DeviceCodeCredentials credentials = DeviceCodeCredentials.ofRefreshToken(
                clientId, clientSecret,
                ignored -> System.out.println("(stored token expired - rerun auth-bootstrap)"),
                storedRefreshToken);
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            printOffer("read", client.offers().get(offerId));

            String newPrice = System.getProperty(NEW_PRICE_PROPERTY);
            if (newPrice != null) {
                client.offers().changeBuyNowPrice(offerId, Money.of(newPrice, CURRENCY_PLN));
                System.out.println("changeBuyNowPrice submitted: " + newPrice + " " + CURRENCY_PLN);
                printOffer("read-back", client.offers().get(offerId));
            }
            rotateToken(tokenStore, account, client);
        }
    }

    private static void printOffer(String phase, Offer offer) {
        String price = offer.buyNowPrice() == null ? "(no Buy Now price)"
                : offer.buyNowPrice().amount() + " " + offer.buyNowPrice().currency();
        System.out.println(phase + ": id=" + offer.id() + ", status=" + offer.status()
                + ", format=" + offer.format() + ", buyNow=" + price);
    }

    private static void rotateToken(SharedTokenStore tokenStore, String account,
            AllegroClient client) throws IOException {
        String rotated = client.refreshToken();
        if (rotated != null) {
            tokenStore.store(account, rotated);
        }
    }
}
