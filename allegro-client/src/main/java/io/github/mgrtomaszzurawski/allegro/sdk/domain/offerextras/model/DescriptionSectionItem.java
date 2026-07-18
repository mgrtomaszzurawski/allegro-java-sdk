/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model;

import io.github.mgrtomaszzurawski.allegro.client.model.DescriptionSectionItemImageRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.DescriptionSectionItemRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.DescriptionSectionItemTextRaw;
import org.jspecify.annotations.Nullable;

/**
 * One item within a description section: either a text block or an image. Build
 * one for a write with {@link #text(String)} or {@link #image(String)}; on a read
 * an item Allegro added after this SDK version degrades to {@link
 * DescriptionItemType#UNKNOWN} with both payload fields {@code null}.
 *
 * @param type the item kind
 * @param content the text content when {@code type} is {@link
 *     DescriptionItemType#TEXT}, otherwise {@code null}
 * @param url the image URL when {@code type} is {@link DescriptionItemType#IMAGE},
 *     otherwise {@code null}
 *
 * @since 0.2.0
 */
public record DescriptionSectionItem(
        DescriptionItemType type,
        @Nullable String content,
        @Nullable String url) {

    /**
     * A text item.
     *
     * @param content the text content
     * @return a text description item
     */
    public static DescriptionSectionItem text(String content) {
        return new DescriptionSectionItem(DescriptionItemType.TEXT, content, null);
    }

    /**
     * An image item.
     *
     * @param url the image URL
     * @return an image description item
     */
    public static DescriptionSectionItem image(String url) {
        return new DescriptionSectionItem(DescriptionItemType.IMAGE, null, url);
    }

    /** Map one generated Layer-1 description item to the public record. */
    static DescriptionSectionItem from(DescriptionSectionItemRaw raw) {
        if (raw instanceof DescriptionSectionItemTextRaw textRaw) {
            return new DescriptionSectionItem(DescriptionItemType.TEXT, textRaw.getContent(), null);
        }
        if (raw instanceof DescriptionSectionItemImageRaw imageRaw) {
            return new DescriptionSectionItem(DescriptionItemType.IMAGE, null, imageRaw.getUrl());
        }
        // C4 forward-compat: the shared UnknownSubtypeToBaseHandler degrades an
        // item type Allegro introduced after this SDK version to the polymorphic
        // base, so expose it as UNKNOWN rather than failing the whole read.
        return new DescriptionSectionItem(DescriptionItemType.UNKNOWN, null, null);
    }
}
