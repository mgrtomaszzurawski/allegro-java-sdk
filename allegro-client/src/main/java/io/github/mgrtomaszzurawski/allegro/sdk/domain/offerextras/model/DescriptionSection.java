/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model;

import io.github.mgrtomaszzurawski.allegro.client.model.DescriptionSectionRaw;
import java.util.List;

/**
 * One section of a standardized offer description — an ordered group of text and
 * image {@link DescriptionSectionItem items}.
 *
 * @param items the section's items in order; never {@code null}, possibly empty
 *
 * @since 0.2.0
 */
public record DescriptionSection(List<DescriptionSectionItem> items) {

    public DescriptionSection {
        items = List.copyOf(items);
    }

    /**
     * A section holding the given items.
     *
     * @param items the section's items in order
     * @return a description section
     */
    public static DescriptionSection of(DescriptionSectionItem... items) {
        return new DescriptionSection(List.of(items));
    }

    /** Map one generated Layer-1 section to the public record. */
    static DescriptionSection from(DescriptionSectionRaw raw) {
        return new DescriptionSection(raw.getItems() == null
                ? List.of()
                : raw.getItems().stream().map(DescriptionSectionItem::from).toList());
    }
}
