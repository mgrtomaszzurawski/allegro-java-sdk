/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.client.model.CompatibilityListIdItemRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CompatibilityListItemRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CompatibilityListTextItemRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.CompatibilityEntry;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompatibilityEntryTest {

    private static final String TEXT_LINE = "CITROEN C6 (TD_) 2005/09-2011/12 2.7 HDi 204KM/150kW";
    private static final String PRODUCT_ID = "d04e8a0c-40a1-4c53-8902-ffee7261845e";
    private static final String INFO_VALUE = "engine-2.7-HDi";

    @Test
    void text_whenBuilt_producesTextUnionItemWithTypeAndText() {
        // when
        CompatibilityEntry entry = CompatibilityEntry.text(TEXT_LINE);

        // then — the entry is a text kind and serializes to a TEXT union item carrying the line
        assertEquals(CompatibilityEntry.Kind.TEXT, entry.kind());
        assertEquals(TEXT_LINE, entry.text());
        CompatibilityListItemRaw raw = entry.toRaw();
        CompatibilityListTextItemRaw item =
                assertInstanceOf(CompatibilityListTextItemRaw.class, raw.getActualInstance());
        assertEquals(CompatibilityListTextItemRaw.TypeEnum.TEXT, item.getType());
        assertEquals(TEXT_LINE, item.getText());
    }

    @Test
    void productId_whenNoAdditionalInfo_producesIdItemWithoutInfo() {
        // when
        CompatibilityEntry entry = CompatibilityEntry.productId(PRODUCT_ID);

        // then — an ID union item with the identifier and no additional-info values
        assertEquals(CompatibilityEntry.Kind.PRODUCT_ID, entry.kind());
        assertEquals(PRODUCT_ID, entry.productId());
        assertTrue(entry.additionalInfo().isEmpty());
        CompatibilityListItemRaw raw = entry.toRaw();
        CompatibilityListIdItemRaw item =
                assertInstanceOf(CompatibilityListIdItemRaw.class, raw.getActualInstance());
        assertEquals(CompatibilityListIdItemRaw.TypeEnum.ID, item.getType());
        assertEquals(PRODUCT_ID, item.getId());
        assertTrue(item.getAdditionalInfo() == null || item.getAdditionalInfo().isEmpty());
    }

    @Test
    void productId_whenAdditionalInfo_producesIdItemCarryingTheInfoValues() {
        // when
        CompatibilityEntry entry = CompatibilityEntry.productId(PRODUCT_ID, List.of(INFO_VALUE));

        // then — the additional-info value is carried on the id item
        CompatibilityListIdItemRaw item =
                assertInstanceOf(CompatibilityListIdItemRaw.class, entry.toRaw().getActualInstance());
        assertEquals(1, item.getAdditionalInfo().size());
        assertEquals(INFO_VALUE, item.getAdditionalInfo().get(0).getValue());
    }

    @Test
    void productId_whenAdditionalInfoMutatedAfterConstruction_entryIsUnaffected() {
        // given
        List<String> info = new ArrayList<>();
        info.add(INFO_VALUE);
        CompatibilityEntry entry = CompatibilityEntry.productId(PRODUCT_ID, info);

        // when — the caller mutates the source list afterwards
        info.clear();

        // then — the entry kept its own copy
        assertEquals(1, entry.additionalInfo().size());
        assertEquals(INFO_VALUE, entry.additionalInfo().get(0));
    }

    @Test
    void text_whenTextNull_throws() {
        assertThrows(NullPointerException.class, () -> CompatibilityEntry.text(null));
    }

    @Test
    void productId_whenIdNull_throws() {
        assertThrows(NullPointerException.class, () -> CompatibilityEntry.productId(null));
    }
}
