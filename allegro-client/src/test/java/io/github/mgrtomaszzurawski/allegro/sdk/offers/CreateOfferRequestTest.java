/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.CreateOfferRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.AfterSalesServices;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferDelivery;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferFormat;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.StockUnit;
import java.util.List;
import org.junit.jupiter.api.Test;

class CreateOfferRequestTest {

    private static final String NAME = "Mechanical keyboard";
    private static final String CATEGORY_ID = "257";
    private static final Money PRICE = Money.of("199.99", "PLN");
    private static final int STOCK = 10;
    private static final String IMAGE_URL = "https://img.example/x.jpg";
    private static final String SHIPPING_RATES_ID = "a1b2c3d4-0000-0000-0000-000000000001";
    private static final String IMPLIED_WARRANTY_ID = "11111111-1111-1111-1111-111111111111";

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
    void build_whenDeliveryAndAfterSalesSet_exposesThem() {
        // given
        OfferDelivery delivery = OfferDelivery.builder().shippingRatesId(SHIPPING_RATES_ID).build();
        AfterSalesServices afterSales =
                AfterSalesServices.builder().impliedWarrantyId(IMPLIED_WARRANTY_ID).build();

        // when
        CreateOfferRequest request = validBuilder()
                .delivery(delivery).afterSalesServices(afterSales).build();

        // then
        assertEquals(delivery, request.delivery());
        assertEquals(afterSales, request.afterSalesServices());
    }

    @Test
    void build_whenFulfilmentNotSet_leavesThoseFieldsNull() {
        // when
        CreateOfferRequest request = validBuilder().build();

        // then — optional fulfilment blocks default to null (omitted from the wire)
        assertNull(request.delivery());
        assertNull(request.afterSalesServices());
    }

    @Test
    void build_whenSellingTermsSet_exposesFormatPricesAndUnit() {
        // given — an auction with starting and minimal prices, counted in pairs
        Money starting = Money.of("1.00", "PLN");
        Money minimal = Money.of("150.00", "PLN");

        // when
        CreateOfferRequest request = validBuilder()
                .sellingFormat(OfferFormat.AUCTION)
                .startingPrice(starting)
                .minimalPrice(minimal)
                .stockUnit(StockUnit.PAIR)
                .build();

        // then
        assertEquals(OfferFormat.AUCTION, request.sellingFormat());
        assertEquals(starting, request.startingPrice());
        assertEquals(minimal, request.minimalPrice());
        assertEquals(StockUnit.PAIR, request.stockUnit());
    }

    @Test
    void build_whenSellingTermsNotSet_leavesThemNull() {
        // when
        CreateOfferRequest request = validBuilder().build();

        // then — format/unit default at the mapper (BUY_NOW/UNIT), so the builder keeps null
        assertNull(request.sellingFormat());
        assertNull(request.startingPrice());
        assertNull(request.minimalPrice());
        assertNull(request.stockUnit());
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
