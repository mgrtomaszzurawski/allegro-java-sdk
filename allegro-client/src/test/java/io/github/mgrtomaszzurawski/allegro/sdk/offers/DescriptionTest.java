/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mgrtomaszzurawski.allegro.client.model.DescriptionSectionItemRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.StandardizedDescriptionRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.DescriptionItem;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.DescriptionItemType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.DescriptionSection;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferDescription;
import org.junit.jupiter.api.Test;

class DescriptionTest {

    private static final String TEXT_CONTENT = "<h1>Mechanical keyboard</h1>";
    private static final String IMAGE_URL = "https://img.example/keyboard.jpg";
    private static final String UNKNOWN_TYPE = "VIDEO";

    @Test
    void text_whenBuilt_carriesContentAndTextType() {
        DescriptionItem item = DescriptionItem.text(TEXT_CONTENT);
        assertEquals(DescriptionItemType.TEXT, item.type());
        assertEquals(TEXT_CONTENT, item.content());
        assertNull(item.url());
    }

    @Test
    void image_whenBuilt_carriesUrlAndImageType() {
        DescriptionItem item = DescriptionItem.image(IMAGE_URL);
        assertEquals(DescriptionItemType.IMAGE, item.type());
        assertEquals(IMAGE_URL, item.url());
        assertNull(item.content());
    }

    @Test
    void from_whenBaseTypeOnly_degradesToUnknown() {
        // given — an item kind added after this SDK release: the C4 handler resolves the
        // unknown subtype to the polymorphic base rather than throwing
        DescriptionSectionItemRaw raw = new DescriptionSectionItemRaw().type(UNKNOWN_TYPE);

        // when
        DescriptionItem item = DescriptionItem.from(raw);

        // then — the mapping degrades to UNKNOWN with no content/url, no exception
        assertEquals(DescriptionItemType.UNKNOWN, item.type());
        assertNull(item.content());
        assertNull(item.url());
    }

    @Test
    void toRaw_whenUnknownItem_throwsBecauseItIsReadOnly() {
        DescriptionItem unknown = DescriptionItem.from(new DescriptionSectionItemRaw().type(UNKNOWN_TYPE));
        assertThrows(IllegalStateException.class, unknown::toRaw);
    }

    @Test
    void description_whenRoundTripped_preservesSectionsAndItems() {
        // given — a two-section description mixing a text and an image item
        OfferDescription original = OfferDescription.of(
                DescriptionSection.of(DescriptionItem.text(TEXT_CONTENT)),
                DescriptionSection.of(DescriptionItem.image(IMAGE_URL)));

        // when — serialize to the wire shape and read it straight back
        StandardizedDescriptionRaw raw = original.toRaw();
        OfferDescription roundTripped = OfferDescription.from(raw);

        // then — value equality holds through the full round trip
        assertEquals(original, roundTripped);
    }

    @Test
    void from_whenNull_returnsNull() {
        assertNull(OfferDescription.from(null));
    }
}
