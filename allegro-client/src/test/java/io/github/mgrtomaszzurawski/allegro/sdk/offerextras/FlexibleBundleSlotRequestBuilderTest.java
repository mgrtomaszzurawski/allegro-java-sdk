/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offerextras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.FlexibleBundleOfferRef;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.FlexibleBundleSlotRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Round-trip and per-required-field failure coverage of
 * {@link FlexibleBundleSlotRequest#builder()}: the mandatory order, required
 * quantity, and at-least-one-offer, plus the optional slot id and entry-point
 * flag.
 */
class FlexibleBundleSlotRequestBuilderTest {

    private static final String SLOT_ID = "22222222-2222-2222-2222-222222222222";
    private static final String OFFER_ID = "offer-1";
    private static final int ORDER = 0;
    private static final int REQUIRED_QUANTITY = 1;
    private static final int NEGATIVE_ORDER = -1;
    private static final int ZERO_QUANTITY = 0;

    private static final String ORDER_REQUIRED_MESSAGE = "order is required";
    private static final String ORDER_NEGATIVE_MESSAGE = "order must be 0 or greater";
    private static final String QUANTITY_REQUIRED_MESSAGE = "requiredQuantity is required";
    private static final String QUANTITY_MIN_MESSAGE = "requiredQuantity must be 1 or greater";
    private static final String NO_OFFERS_MESSAGE = "a slot must have at least one offer";

    private static FlexibleBundleSlotRequest validSlot() {
        return FlexibleBundleSlotRequest.builder()
                .order(ORDER)
                .entryPoint(true)
                .requiredQuantity(REQUIRED_QUANTITY)
                .offer(FlexibleBundleOfferRef.of(OFFER_ID, false))
                .build();
    }

    @Test
    void build_whenRequiredFieldsSet_keepsFields() {
        // when
        FlexibleBundleSlotRequest slot = FlexibleBundleSlotRequest.builder()
                .id(SLOT_ID)
                .order(ORDER)
                .entryPoint(true)
                .requiredQuantity(REQUIRED_QUANTITY)
                .offer(FlexibleBundleOfferRef.of(OFFER_ID, true))
                .build();

        // then
        assertEquals(SLOT_ID, slot.id());
        assertEquals(ORDER, slot.order());
        assertTrue(slot.entryPoint());
        assertEquals(REQUIRED_QUANTITY, slot.requiredQuantity());
        assertEquals(1, slot.offers().size());
        assertEquals(OFFER_ID, slot.offers().get(0).offerId());
        assertTrue(slot.offers().get(0).excludedFromDiscount());
    }

    @Test
    void toBuilder_whenRebuilt_preservesFields() {
        // given
        FlexibleBundleSlotRequest original = validSlot();

        // when
        FlexibleBundleSlotRequest copy = original.toBuilder().build();

        // then
        assertEquals(original, copy);
    }

    @Test
    void build_whenOrderMissing_throwsIllegalState() {
        var builder = FlexibleBundleSlotRequest.builder()
                .requiredQuantity(REQUIRED_QUANTITY)
                .offer(FlexibleBundleOfferRef.of(OFFER_ID, false));

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertEquals(ORDER_REQUIRED_MESSAGE, failure.getMessage());
    }

    @Test
    void build_whenOrderNegative_throwsIllegalState() {
        var builder = FlexibleBundleSlotRequest.builder()
                .order(NEGATIVE_ORDER)
                .requiredQuantity(REQUIRED_QUANTITY)
                .offer(FlexibleBundleOfferRef.of(OFFER_ID, false));

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertEquals(ORDER_NEGATIVE_MESSAGE, failure.getMessage());
    }

    @Test
    void build_whenRequiredQuantityMissing_throwsIllegalState() {
        var builder = FlexibleBundleSlotRequest.builder()
                .order(ORDER)
                .offer(FlexibleBundleOfferRef.of(OFFER_ID, false));

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertEquals(QUANTITY_REQUIRED_MESSAGE, failure.getMessage());
    }

    @Test
    void build_whenRequiredQuantityBelowOne_throwsIllegalState() {
        var builder = FlexibleBundleSlotRequest.builder()
                .order(ORDER)
                .requiredQuantity(ZERO_QUANTITY)
                .offer(FlexibleBundleOfferRef.of(OFFER_ID, false));

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertEquals(QUANTITY_MIN_MESSAGE, failure.getMessage());
    }

    @Test
    void build_whenNoOffers_throwsIllegalState() {
        var builder = FlexibleBundleSlotRequest.builder()
                .order(ORDER)
                .requiredQuantity(REQUIRED_QUANTITY);

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertEquals(NO_OFFERS_MESSAGE, failure.getMessage());
    }

    @Test
    void offerRefOf_whenOfferIdNull_throwsNpe() {
        // then — the required offer id is fail-fast at construction
        assertThrows(NullPointerException.class, () -> FlexibleBundleOfferRef.of(null, false));
    }

    @Test
    void build_whenOffersListSet_replacesAccumulatedOffers() {
        // when — offers(List) sets the whole list
        FlexibleBundleSlotRequest slot = FlexibleBundleSlotRequest.builder()
                .order(ORDER)
                .requiredQuantity(REQUIRED_QUANTITY)
                .offers(List.of(
                        FlexibleBundleOfferRef.of(OFFER_ID, false),
                        FlexibleBundleOfferRef.of("offer-2", true)))
                .build();

        // then
        assertEquals(2, slot.offers().size());
    }
}
