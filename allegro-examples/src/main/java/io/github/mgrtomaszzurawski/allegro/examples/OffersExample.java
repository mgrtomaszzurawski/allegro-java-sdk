/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.examples;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.OfferFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.BatchReport;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.Offer;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferFormat;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferSummary;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.SmartClassification;
import java.util.List;

/**
 * Compile-only twin of the {@code docs/offers.md} snippets — if the documented
 * offers usage stops compiling, this module breaks the build.
 */
public final class OffersExample {

    private OffersExample() {
    }

    static String readOffer(AllegroClient client, String offerId) {
        Offer offer = client.offers().get(offerId);
        if (offer.buyNowPrice() != null) {
            Money price = offer.buyNowPrice();
            return offer.name() + " — " + price.amount() + " " + price.currency();
        }
        return offer.name() + " — " + offer.status();
    }

    static void changePrice(AllegroClient client, String offerId) {
        client.offers().changeBuyNowPrice(offerId, Money.of("149.50", "PLN"));
    }

    static List<OfferSummary> listActiveBuyNowOffers(AllegroClient client) {
        OfferFilter filter = OfferFilter.builder()
                .status(OfferStatus.ACTIVE)
                .format(OfferFormat.BUY_NOW)
                .build();
        return client.offers().streamOffers(filter).limit(50).toList();
    }

    static long unmetSmartConditions(AllegroClient client, String offerId) {
        SmartClassification smart = client.offers().smartClassification(offerId);
        return smart.conditions().stream()
                .filter(condition -> !condition.fulfilled())
                .count();
    }

    static int publishOffers(AllegroClient client, List<String> offerIds) {
        BatchReport report = client.offers().batch().publish(offerIds);
        return report.success();
    }

    static int repriceOffers(AllegroClient client, List<String> offerIds) {
        BatchReport report = client.offers().batch().changePrices(offerIds, Money.of("129.00", "PLN"));
        return report.success();
    }
}
