/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.CreateOfferRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.OfferFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.BatchReport;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.Offer;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferSummary;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.SmartClassification;
import java.io.IOException;
import java.util.List;

/**
 * Bucket A sandbox probe. With no {@code -Pdemo.offerId} it lists the seller's
 * offers via the lazy {@code streamOffers} read; with an id it reads that offer
 * plus its Smart! classification, and — when {@code -Pdemo.newPrice} is also
 * given — runs the write→read cycle (change the Buy Now price through the SDK,
 * then read it back and confirm the round-trip).
 *
 * <pre>
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=offer                        # list my offers
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=offer -Pdemo.offerId=13579
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=offer -Pdemo.offerId=13579 -Pdemo.newPrice=149.50
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=offer -Pdemo.publishIds=13579,24680  # bulk publish
 * </pre>
 */
final class OffersDemo {

    private static final String OFFER_ID_PROPERTY = "demo.offerId";
    private static final String NEW_PRICE_PROPERTY = "demo.newPrice";
    private static final String PUBLISH_IDS_PROPERTY = "demo.publishIds";
    private static final String CREATE_NAME_PROPERTY = "demo.createName";
    private static final String CREATE_CATEGORY_PROPERTY = "demo.createCategory";
    private static final String CREATE_PRICE_PROPERTY = "demo.createPrice";
    private static final String CREATE_STOCK_PROPERTY = "demo.createStock";
    private static final String CURRENCY_PLN = "PLN";
    private static final String OFFER_ID_SEPARATOR = ",";
    private static final int STREAM_LIMIT = 10;
    private static final String ERR_NO_STORED_TOKEN =
            "No stored refresh token for account '%s' - run the auth-bootstrap scenario first";

    private OffersDemo() {
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
                ignored -> System.out.println("(stored token expired - rerun auth-bootstrap)"),
                storedRefreshToken);
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            String offerId = System.getProperty(OFFER_ID_PROPERTY);
            String publishIds = System.getProperty(PUBLISH_IDS_PROPERTY);
            String createName = System.getProperty(CREATE_NAME_PROPERTY);
            if (createName != null) {
                createOffer(client, createName);
            } else if (publishIds != null) {
                publishBatch(client, publishIds);
            } else if (offerId == null) {
                streamOffers(client);
            } else {
                printOffer("read", client.offers().get(offerId));
                printSmart(client, offerId);
                String newPrice = System.getProperty(NEW_PRICE_PROPERTY);
                if (newPrice != null) {
                    client.offers().changeBuyNowPrice(offerId, Money.of(newPrice, CURRENCY_PLN));
                    System.out.println("changeBuyNowPrice submitted: " + newPrice + " " + CURRENCY_PLN);
                    printOffer("read-back", client.offers().get(offerId));
                }
            }
            rotateToken(tokenStore, account, client);
        }
    }

    private static void streamOffers(AllegroClient client) {
        List<OfferSummary> firstOffers = client.offers().streamOffers(OfferFilter.all())
                .limit(STREAM_LIMIT).toList();
        System.out.println("streamOffers: first " + firstOffers.size() + " offer(s)");
        for (OfferSummary summary : firstOffers) {
            String price = summary.buyNowPrice() == null ? "(no Buy Now price)"
                    : summary.buyNowPrice().amount() + " " + summary.buyNowPrice().currency();
            System.out.println("  id=" + summary.id() + ", status=" + summary.status()
                    + ", format=" + summary.format() + ", stock=" + summary.availableStock()
                    + ", buyNow=" + price);
        }
    }

    private static void createOffer(AllegroClient client, String name) {
        CreateOfferRequest request = CreateOfferRequest.builder()
                .name(name)
                .categoryId(System.getProperty(CREATE_CATEGORY_PROPERTY))
                .buyNowPrice(Money.of(System.getProperty(CREATE_PRICE_PROPERTY), CURRENCY_PLN))
                .availableStock(Integer.parseInt(System.getProperty(CREATE_STOCK_PROPERTY)))
                .build();
        try {
            Offer created = client.offers().create(request);
            System.out.println("create: id=" + created.id() + ", status=" + created.status()
                    + ", name=" + created.name());
        } catch (AllegroBadRequestException e) {
            System.out.println("create rejected — " + e.errors().size() + " field error(s):");
            e.errors().forEach(fieldError -> System.out.println("  - path=" + fieldError.path()
                    + " code=" + fieldError.code() + " userMessage=" + fieldError.userMessage()));
        }
    }

    private static void publishBatch(AllegroClient client, String csvOfferIds) {
        List<String> offerIds = List.of(csvOfferIds.split(OFFER_ID_SEPARATOR));
        BatchReport report = client.offers().batch().publish(offerIds);
        System.out.println("batch publish: " + report.success() + "/" + report.total()
                + " ok, " + report.failed() + " failed");
    }

    private static void printSmart(AllegroClient client, String offerId) {
        SmartClassification smart = client.offers().smartClassification(offerId);
        System.out.println("smart: fulfilled=" + smart.fulfilled()
                + ", scheduledForReclassification=" + smart.scheduledForReclassification()
                + ", conditions=" + smart.conditions().size());
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
