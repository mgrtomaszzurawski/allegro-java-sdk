/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mgrtomaszzurawski.allegro.client.model.JustIdRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferStatusRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferPublicationMarketplacesResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferPublicationResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferPublication;
import org.junit.jupiter.api.Test;

class OfferPublicationTest {

    private static final String BASE_MARKETPLACE = "allegro-pl";

    @Test
    void from_whenNull_returnsNull() {
        // given no publication block; then the value is absent, not an empty object
        assertNull(OfferPublication.from(null));
    }

    @Test
    void from_whenPopulated_mapsRepublishAndBaseMarketplace() {
        // given a publication with a republish flag and a base marketplace
        SaleProductOfferPublicationResponseRaw raw = new SaleProductOfferPublicationResponseRaw()
                .status(OfferStatusRaw.ACTIVE)
                .republish(Boolean.TRUE)
                .marketplaces(new SaleProductOfferPublicationMarketplacesResponseRaw()
                        .base(new JustIdRaw().id(BASE_MARKETPLACE)));

        // when projected onto the consumer value
        OfferPublication publication = OfferPublication.from(raw);

        // then the surrounding lifecycle data maps (status stays on the offer, not here)
        assertEquals(Boolean.TRUE, publication.republish());
        assertEquals(BASE_MARKETPLACE, publication.baseMarketplaceId());
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
    }
}
