/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mgrtomaszzurawski.allegro.client.model.OfferCategoryRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferStatusRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferPublicationResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferResponseV1Raw;
import io.github.mgrtomaszzurawski.allegro.client.model.SellingModeFormatRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SellingModeRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.Offer;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferFormat;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferStatus;
import org.junit.jupiter.api.Test;

class OfferTest {

    private static final String OFFER_ID = "13579";
    private static final String CATEGORY_ID = "257";

    @Test
    void from_whenFormatAndStatusAbsent_mapsBothToUnknown() {
        // given — a payload whose selling-mode format and publication status are
        // absent (an as-yet-unmodelled or partial state)
        SaleProductOfferResponseV1Raw raw = new SaleProductOfferResponseV1Raw()
                .id(OFFER_ID)
                .name("Partial")
                .category(new OfferCategoryRaw().id(CATEGORY_ID))
                .sellingMode(new SellingModeRaw())
                .publication(new SaleProductOfferPublicationResponseRaw());

        // when
        Offer offer = Offer.from(raw);

        // then — unknown enum values fall back rather than throwing, price/stock null
        assertEquals(OfferFormat.UNKNOWN, offer.format());
        assertEquals(OfferStatus.UNKNOWN, offer.status());
        assertNull(offer.buyNowPrice());
        assertNull(offer.availableStock());
    }

    @Test
    void from_whenFormatAndStatusCarryUnknownWireValue_degradesInsteadOfThrowing() {
        // given — Allegro returns a selling-mode format and a status value added
        // after this SDK release (a value the generated enums do not model)
        SellingModeFormatRaw unknownFormat =
                assertDoesNotThrow(() -> SellingModeFormatRaw.fromValue("FUTURE_FORMAT"));
        OfferStatusRaw unknownStatus =
                assertDoesNotThrow(() -> OfferStatusRaw.fromValue("FUTURE_STATUS"));

        // then — Layer 1 returns the forward-compat sentinel instead of throwing
        // (enumUnknownDefaultCase), and the domain mapping degrades it to UNKNOWN
        // rather than failing the whole response deserialization
        assertEquals(SellingModeFormatRaw.UNKNOWN_DEFAULT_OPEN_API, unknownFormat);
        assertEquals(OfferStatusRaw.UNKNOWN_DEFAULT_OPEN_API, unknownStatus);
        assertEquals(OfferFormat.UNKNOWN, OfferFormat.from(unknownFormat));
        assertEquals(OfferStatus.UNKNOWN, OfferStatus.from(unknownStatus));
    }

    @Test
    void from_whenNestedObjectsAbsent_toleratesWithoutNullPointer() {
        // given — a payload with no selling mode, category or publication at all
        // (the spec marks none of them required, so mapping must not dereference)
        SaleProductOfferResponseV1Raw raw = new SaleProductOfferResponseV1Raw()
                .id(OFFER_ID)
                .name("Bare");

        // when
        Offer offer = Offer.from(raw);

        // then — every optional projection degrades to null/UNKNOWN, no NPE
        assertEquals(OFFER_ID, offer.id());
        assertNull(offer.categoryId());
        assertEquals(OfferFormat.UNKNOWN, offer.format());
        assertEquals(OfferStatus.UNKNOWN, offer.status());
        assertNull(offer.buyNowPrice());
        assertNull(offer.availableStock());
    }
}
