/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalMarketplacePublicationStateRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalMarketplacesResponseValuePublicationRaw.StateEnum;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.MarketplacePublicationState;
import org.junit.jupiter.api.Test;

/** Mapping of the marketplace publication-state enum from the generated wire enum. */
class MarketplacePublicationStateTest {

    @Test
    void from_whenNull_returnsNull() {
        assertNull(MarketplacePublicationState.from((StateEnum) null));
    }

    @Test
    void from_mapsEachKnownWireValue() {
        assertEquals(MarketplacePublicationState.APPROVED, MarketplacePublicationState.from(StateEnum.APPROVED));
        assertEquals(MarketplacePublicationState.REFUSED, MarketplacePublicationState.from(StateEnum.REFUSED));
        assertEquals(MarketplacePublicationState.IN_PROGRESS,
                MarketplacePublicationState.from(StateEnum.IN_PROGRESS));
        assertEquals(MarketplacePublicationState.NOT_REQUESTED,
                MarketplacePublicationState.from(StateEnum.NOT_REQUESTED));
        assertEquals(MarketplacePublicationState.PENDING, MarketplacePublicationState.from(StateEnum.PENDING));
    }

    @Test
    void from_whenUnknownSentinel_mapsToUnknown() {
        // then an unmodelled future state degrades to UNKNOWN, not an exception
        assertEquals(MarketplacePublicationState.UNKNOWN,
                MarketplacePublicationState.from(StateEnum.UNKNOWN_DEFAULT_OPEN_API));
    }

    @Test
    void from_whenListingStateNull_returnsNull() {
        assertNull(MarketplacePublicationState.from((AdditionalMarketplacePublicationStateRaw) null));
    }

    @Test
    void from_mapsEachKnownListingWireValue() {
        assertEquals(MarketplacePublicationState.APPROVED,
                MarketplacePublicationState.from(AdditionalMarketplacePublicationStateRaw.APPROVED));
        assertEquals(MarketplacePublicationState.REFUSED,
                MarketplacePublicationState.from(AdditionalMarketplacePublicationStateRaw.REFUSED));
        assertEquals(MarketplacePublicationState.IN_PROGRESS,
                MarketplacePublicationState.from(AdditionalMarketplacePublicationStateRaw.IN_PROGRESS));
        assertEquals(MarketplacePublicationState.NOT_REQUESTED,
                MarketplacePublicationState.from(AdditionalMarketplacePublicationStateRaw.NOT_REQUESTED));
        assertEquals(MarketplacePublicationState.PENDING,
                MarketplacePublicationState.from(AdditionalMarketplacePublicationStateRaw.PENDING));
    }

    @Test
    void from_whenListingUnknownSentinel_mapsToUnknown() {
        assertEquals(MarketplacePublicationState.UNKNOWN,
                MarketplacePublicationState.from(AdditionalMarketplacePublicationStateRaw.UNKNOWN_DEFAULT_OPEN_API));
    }
}
