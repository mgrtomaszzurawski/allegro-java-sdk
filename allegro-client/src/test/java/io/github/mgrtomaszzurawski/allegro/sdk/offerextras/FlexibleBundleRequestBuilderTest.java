/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offerextras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.FlexibleBundleOfferRef;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.FlexibleBundleRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.FlexibleBundleSlotRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.FlexibleBundleDiscount;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.MarketplaceDiscount;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.WholeBundleDiscount;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Round-trip and failure coverage of {@link FlexibleBundleRequest#builder()}: the
 * mandatory at-least-one-slot, the optional discount, and the {@code toBuilder}
 * copy.
 */
class FlexibleBundleRequestBuilderTest {

    private static final String OFFER_ID = "offer-1";
    private static final int ORDER = 0;
    private static final int REQUIRED_QUANTITY = 1;
    private static final int MIN_BOUGHT = 2;
    private static final int PERCENTAGE = 10;
    private static final String MARKETPLACE = "allegro-pl";
    private static final String NO_SLOTS_MESSAGE = "a flexible bundle must have at least one slot";

    private static FlexibleBundleSlotRequest slot() {
        return FlexibleBundleSlotRequest.builder()
                .order(ORDER)
                .requiredQuantity(REQUIRED_QUANTITY)
                .offer(FlexibleBundleOfferRef.of(OFFER_ID, false))
                .build();
    }

    private static FlexibleBundleDiscount discount() {
        return FlexibleBundleDiscount.wholeBundle(
                new WholeBundleDiscount(MIN_BOUGHT, List.of(new MarketplaceDiscount(MARKETPLACE, PERCENTAGE))));
    }

    @Test
    void build_whenSlotOnly_keepsSlotAndNoDiscount() {
        // when
        FlexibleBundleRequest request = FlexibleBundleRequest.builder().slot(slot()).build();

        // then
        assertEquals(1, request.slots().size());
        assertNull(request.discount());
    }

    @Test
    void build_whenSlotAndDiscount_keepsBoth() {
        // when
        FlexibleBundleRequest request = FlexibleBundleRequest.builder()
                .slot(slot())
                .discount(discount())
                .build();

        // then
        assertEquals(1, request.slots().size());
        assertEquals(discount(), request.discount());
    }

    @Test
    void toBuilder_whenRebuilt_preservesSlotsAndDiscount() {
        // given
        FlexibleBundleRequest original = FlexibleBundleRequest.builder()
                .slot(slot())
                .discount(discount())
                .build();

        // when
        FlexibleBundleRequest copy = original.toBuilder().build();

        // then
        assertEquals(original, copy);
    }

    @Test
    void build_whenNoSlots_throwsIllegalState() {
        var builder = FlexibleBundleRequest.builder();

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertEquals(NO_SLOTS_MESSAGE, failure.getMessage());
    }
}
