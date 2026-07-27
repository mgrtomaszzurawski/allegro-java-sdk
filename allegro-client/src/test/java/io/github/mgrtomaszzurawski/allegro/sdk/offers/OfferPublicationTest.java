/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.client.model.JustIdRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferStatusRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferPublicationMarketplacesResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferPublicationResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferPublication;
import java.util.List;
import org.junit.jupiter.api.Test;

class OfferPublicationTest {

    private static final String BASE_MARKETPLACE = "allegro-pl";
    private static final String ADDITIONAL_MARKETPLACE = "allegro-cz";
    private static final String DURATION = "PT720H";

    @Test
    void from_whenNull_returnsNull() {
        // given no publication block; then the value is absent, not an empty object
        assertNull(OfferPublication.from(null));
    }

    @Test
    void from_whenPopulated_mapsAllPublicationFields() {
        // given a publication with a republish flag and a base marketplace
        SaleProductOfferPublicationResponseRaw raw = new SaleProductOfferPublicationResponseRaw()
                .status(OfferStatusRaw.ACTIVE)
                .republish(Boolean.TRUE)
                .duration(DURATION)
                .marketplaces(new SaleProductOfferPublicationMarketplacesResponseRaw()
                        .base(new JustIdRaw().id(BASE_MARKETPLACE))
                        .additional(List.of(new JustIdRaw().id(ADDITIONAL_MARKETPLACE))));

        // when projected onto the consumer value
        OfferPublication publication = OfferPublication.from(raw);

        // then the surrounding lifecycle data maps (status stays on the offer, not here)
        assertEquals(Boolean.TRUE, publication.republish());
        assertEquals(BASE_MARKETPLACE, publication.baseMarketplaceId());
        assertEquals(DURATION, publication.duration());
        assertEquals(List.of(ADDITIONAL_MARKETPLACE), publication.additionalMarketplaceIds());
        assertNull(publication.endedBy());
    }

    @Test
    void from_whenMarketplacesOmitted_leavesBaseMarketplaceNull() {
        // given a publication with no marketplaces block
        SaleProductOfferPublicationResponseRaw raw = new SaleProductOfferPublicationResponseRaw()
                .republish(Boolean.FALSE);

        // when projected; then the base marketplace is null (no NPE on the missing block)
        OfferPublication publication = OfferPublication.from(raw);
        assertNull(publication.baseMarketplaceId());
        assertEquals(Boolean.FALSE, publication.republish());
        assertNull(publication.duration());
        assertTrue(publication.additionalMarketplaceIds().isEmpty());
    }

    @Test
    void from_whenAdditionalMarketplaceHasNullId_skipsIt() {
        // given an additional marketplace entry with no id (spec-legal: id is not required)
        SaleProductOfferPublicationResponseRaw raw = new SaleProductOfferPublicationResponseRaw()
                .marketplaces(new SaleProductOfferPublicationMarketplacesResponseRaw()
                        .additional(List.of(new JustIdRaw().id(ADDITIONAL_MARKETPLACE), new JustIdRaw())));

        // when projected; then the null-id entry is dropped, the valid id survives
        OfferPublication publication = OfferPublication.from(raw);
        assertEquals(List.of(ADDITIONAL_MARKETPLACE), publication.additionalMarketplaceIds());
    }

    @Test
    void constructor_whenAdditionalMarketplaceIdsNull_defaultsToEmptyImmutableList() {
        // given a direct construction with a null list (the from() path always passes non-null)
        OfferPublication publication =
                new OfferPublication(null, null, null, null, null, null, null);

        // then it is normalized to an empty list, not null
        assertTrue(publication.additionalMarketplaceIds().isEmpty());
    }
}
