/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.PriceChangeRequest;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * Fail-fast validation and accessors of the {@link PriceChangeRequest} builder.
 * One failure test per required field / guard (TESTING.md §1); the wire mapping is
 * covered separately by {@code PriceChangeMapperTest}.
 */
class PriceChangeRequestTest {

    private static final String OFFER_ID = "111";
    private static final String CURRENCY_PLN = "PLN";
    private static final String PRICE = "149.50";
    private static final String MARKETPLACE_PL = "allegro-pl";
    private static final String DELTA = "10.00";
    private static final String SMALL_DELTA = "5.00";
    private static final String ZERO = "0";
    private static final String NEGATIVE = "-1.00";
    private static final String PERCENTAGE = "10";
    private static final String OTHER_OFFER_ID = "999";

    private static Money money(String amount) {
        return Money.of(amount, CURRENCY_PLN);
    }

    @Test
    void forOffers_whenNullOffers_throws() {
        assertThrows(NullPointerException.class, () -> PriceChangeRequest.forOffers(null));
    }

    @Test
    void forOffers_whenEmptyOffers_throws() {
        assertThrows(IllegalArgumentException.class, () -> PriceChangeRequest.forOffers(List.of()));
    }

    @Test
    void forOffers_whenBlankOfferId_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> PriceChangeRequest.forOffers(List.of(" ")));
    }

    @Test
    void forOffers_whenTooManyOffers_throws() {
        List<String> tooMany = IntStream.rangeClosed(0, PriceChangeRequest.MAX_OFFERS)
                .mapToObj(Integer::toString).toList();
        assertThrows(IllegalArgumentException.class, () -> PriceChangeRequest.forOffers(tooMany));
    }

    @Test
    void build_whenNoChange_throws() {
        // given/when/then — no set/increase/decrease was chosen
        PriceChangeRequest.Builder builder = PriceChangeRequest.forOffers(List.of(OFFER_ID));
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void increaseBy_whenFixedAlreadySet_throwsSingleChange() {
        // given — a fixed price already set (the single change)
        PriceChangeRequest.Builder builder = PriceChangeRequest.forOffers(List.of(OFFER_ID))
                .setPrice(money(PRICE));

        // when/then — a second kind of change is rejected
        assertThrows(IllegalStateException.class, () -> builder.increaseBy(money(DELTA)));
    }

    @Test
    void increaseByPercent_whenAmountChangeAlreadySet_throwsSingleChange() {
        // given — an amount increase already set (the single change)
        PriceChangeRequest.Builder builder = PriceChangeRequest.forOffers(List.of(OFFER_ID))
                .increaseBy(money(DELTA));

        // when/then — adding a percentage change too is rejected
        assertThrows(IllegalStateException.class, () -> builder.decreaseByPercent(PERCENTAGE));
    }

    @Test
    void setPrice_whenNotPositive_throws() {
        PriceChangeRequest.Builder builder = PriceChangeRequest.forOffers(List.of(OFFER_ID));
        assertThrows(IllegalArgumentException.class, () -> builder.setPrice(money(ZERO)));
    }

    @Test
    void increaseBy_whenNegative_throws() {
        PriceChangeRequest.Builder builder = PriceChangeRequest.forOffers(List.of(OFFER_ID));
        assertThrows(IllegalArgumentException.class, () -> builder.increaseBy(money(NEGATIVE)));
    }

    @Test
    void decreaseBy_whenNegative_throws() {
        PriceChangeRequest.Builder builder = PriceChangeRequest.forOffers(List.of(OFFER_ID));
        assertThrows(IllegalArgumentException.class, () -> builder.decreaseBy(money(NEGATIVE)));
    }

    @Test
    void increaseByPercent_whenBlank_throws() {
        PriceChangeRequest.Builder builder = PriceChangeRequest.forOffers(List.of(OFFER_ID));
        assertThrows(IllegalArgumentException.class, () -> builder.increaseByPercent(" "));
    }

    @Test
    void onMarketplace_whenBlank_throws() {
        PriceChangeRequest.Builder builder = PriceChangeRequest.forOffers(List.of(OFFER_ID))
                .setPrice(money(PRICE));
        assertThrows(IllegalArgumentException.class, () -> builder.onMarketplace(" "));
    }

    @Test
    void build_whenFixedOnMarketplace_exposesBuiltData() {
        // given — a fixed price on a specific marketplace
        PriceChangeRequest request = PriceChangeRequest.forOffers(List.of(OFFER_ID))
                .setPrice(money(PRICE))
                .onMarketplace(MARKETPLACE_PL)
                .build();

        // then — the accessors read the built intent back
        assertEquals(List.of(OFFER_ID), request.offerIds());
        assertEquals(PriceChangeRequest.Kind.FIXED, request.kind());
        assertEquals(PRICE, request.amount().amount());
        assertEquals(MARKETPLACE_PL, request.marketplaceId());
    }

    @Test
    void build_whenIncreaseWithoutMarketplace_marketplaceIsNull() {
        // given — a relative increase, no marketplace
        PriceChangeRequest request = PriceChangeRequest.forOffers(List.of(OFFER_ID))
                .increaseBy(money(SMALL_DELTA))
                .build();

        // then — kind is INCREASE and marketplace defaults to null (base marketplace)
        assertEquals(PriceChangeRequest.Kind.INCREASE, request.kind());
        assertNull(request.marketplaceId());
    }

    @Test
    void build_whenDecreaseByPercent_exposesPercentageAndNoAmount() {
        // given — a relative percentage decrease
        PriceChangeRequest request = PriceChangeRequest.forOffers(List.of(OFFER_ID))
                .decreaseByPercent(PERCENTAGE)
                .build();

        // then — kind is DECREASE_PERCENTAGE, percentage set, amount null
        assertEquals(PriceChangeRequest.Kind.DECREASE_PERCENTAGE, request.kind());
        assertEquals(PERCENTAGE, request.percentage());
        assertNull(request.amount());
    }

    @Test
    void offerIds_whenReadFromRequest_isImmutable() {
        PriceChangeRequest request = PriceChangeRequest.forOffers(List.of(OFFER_ID))
                .setPrice(money(PRICE))
                .build();
        List<String> offerIds = request.offerIds();
        assertThrows(UnsupportedOperationException.class, () -> offerIds.add(OTHER_OFFER_ID));
    }
}
