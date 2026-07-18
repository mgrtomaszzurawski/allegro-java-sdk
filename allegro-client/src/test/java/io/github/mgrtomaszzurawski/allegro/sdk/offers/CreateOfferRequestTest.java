/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.CreateOfferRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class CreateOfferRequestTest {

    private static final String NAME = "Mechanical keyboard";
    private static final String CATEGORY_ID = "257";
    private static final Money PRICE = Money.of("199.99", "PLN");
    private static final int STOCK = 10;
    private static final String IMAGE_URL = "https://img.example/x.jpg";

    private static CreateOfferRequest.Builder validBuilder() {
        return CreateOfferRequest.builder()
                .name(NAME)
                .categoryId(CATEGORY_ID)
                .buyNowPrice(PRICE)
                .availableStock(STOCK);
    }

    @Test
    void build_whenAllRequiredFieldsSet_exposesEachValue() {
        // when
        CreateOfferRequest request = validBuilder().imageUrls(List.of(IMAGE_URL)).build();

        // then
        assertEquals(NAME, request.name());
        assertEquals(CATEGORY_ID, request.categoryId());
        assertEquals(PRICE, request.buyNowPrice());
        assertEquals(STOCK, request.availableStock());
        assertEquals(List.of(IMAGE_URL), request.imageUrls());
    }

    @Test
    void build_whenNoImages_defaultsToEmptyList() {
        // when
        CreateOfferRequest request = validBuilder().build();

        // then
        assertTrue(request.imageUrls().isEmpty());
    }

    @Test
    void build_whenNameMissing_throwsIllegalState() {
        // given — every required field but name
        CreateOfferRequest.Builder builder = CreateOfferRequest.builder()
                .categoryId(CATEGORY_ID).buyNowPrice(PRICE).availableStock(STOCK);

        // then
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void build_whenCategoryMissing_throwsIllegalState() {
        CreateOfferRequest.Builder builder = CreateOfferRequest.builder()
                .name(NAME).buyNowPrice(PRICE).availableStock(STOCK);
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void build_whenPriceMissing_throwsIllegalState() {
        CreateOfferRequest.Builder builder = CreateOfferRequest.builder()
                .name(NAME).categoryId(CATEGORY_ID).availableStock(STOCK);
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void build_whenStockMissing_throwsIllegalState() {
        // given — availableStock never set
        CreateOfferRequest.Builder builder = CreateOfferRequest.builder()
                .name(NAME).categoryId(CATEGORY_ID).buyNowPrice(PRICE);
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void build_whenStockNegative_throwsIllegalState() {
        CreateOfferRequest.Builder builder = validBuilder().availableStock(-1);
        assertThrows(IllegalStateException.class, builder::build);
    }
}
