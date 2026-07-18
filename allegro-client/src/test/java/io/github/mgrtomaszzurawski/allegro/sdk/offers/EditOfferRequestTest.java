/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.EditOfferRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class EditOfferRequestTest {

    private static final String NAME = "Renamed offer";
    private static final Money PRICE = Money.of("149.00", "PLN");
    private static final int STOCK = 25;
    private static final String IMAGE_URL = "https://img.example/x.jpg";

    @Test
    void build_whenAllFieldsSet_exposesEachValue() {
        // when
        EditOfferRequest request = EditOfferRequest.builder()
                .name(NAME)
                .buyNowPrice(PRICE)
                .availableStock(STOCK)
                .imageUrls(List.of(IMAGE_URL))
                .build();

        // then
        assertEquals(NAME, request.name());
        assertEquals(PRICE, request.buyNowPrice());
        assertEquals(STOCK, request.availableStock());
        assertEquals(List.of(IMAGE_URL), request.imageUrls());
    }

    @Test
    void build_whenNothingSet_leavesEveryFieldNull() {
        // when — an empty partial edit
        EditOfferRequest request = EditOfferRequest.builder().build();

        // then — every field is unset (so the PATCH omits them all)
        assertNull(request.name());
        assertNull(request.buyNowPrice());
        assertNull(request.availableStock());
        assertNull(request.imageUrls());
    }
}
