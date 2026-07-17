/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.examples;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.Offer;

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
}
