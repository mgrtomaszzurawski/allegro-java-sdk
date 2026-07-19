/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.DescriptionSectionItemRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.DescriptionSectionRaw;
import java.util.List;

/**
 * One section of an offer description — an ordered list of {@link DescriptionItem}s
 * (text blocks and images) rendered together. Allegro pairs a text item with an
 * image item within a section for the two-column layout.
 *
 * @param items the section's items, in display order
 * @since 0.3.0
 */
public record DescriptionSection(List<DescriptionItem> items) {

    /** Canonical constructor; defensively copies the items into an immutable list. */
    public DescriptionSection {
        items = List.copyOf(items);
    }

    /** A section of the given items, in order. */
    public static DescriptionSection of(DescriptionItem... items) {
        return new DescriptionSection(List.of(items));
    }

    /** Project a generated description section onto the consumer value. */
    public static DescriptionSection from(DescriptionSectionRaw raw) {
        List<DescriptionSectionItemRaw> rawItems = raw.getItems();
        return new DescriptionSection(rawItems == null
                ? List.of()
                : rawItems.stream().map(DescriptionItem::from).toList());
    }

    /** The generated section for this value. */
    public DescriptionSectionRaw toRaw() {
        return new DescriptionSectionRaw().items(items.stream().map(DescriptionItem::toRaw).toList());
    }
}
