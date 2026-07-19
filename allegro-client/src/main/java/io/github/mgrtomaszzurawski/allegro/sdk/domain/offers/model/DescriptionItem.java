/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.DescriptionSectionItemImageRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.DescriptionSectionItemRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.DescriptionSectionItemTextRaw;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * One item of an offer description section: either a block of formatted (HTML)
 * {@code text} or an {@code image} referenced by URL.
 *
 * <p>Create one with {@link #text(String)} or {@link #image(String)}; read one
 * back from an {@link Offer}'s description. An item Allegro adds after this SDK
 * release reads back with type {@link DescriptionItemType#UNKNOWN} and no content.
 *
 * @param type    which kind of item this is
 * @param content the HTML text for a {@code TEXT} item, else {@code null}
 * @param url     the image URL for an {@code IMAGE} item, else {@code null}
 * @since 0.3.0
 */
public record DescriptionItem(
        DescriptionItemType type,
        @Nullable String content,
        @Nullable String url) {

    private static final String ERR_CONTENT = "content is required for a text item";
    private static final String ERR_URL = "url is required for an image item";
    private static final String ERR_NOT_WRITABLE =
            "a DescriptionItem of UNKNOWN type is read-only and cannot be sent";
    private static final String ERR_TEXT_URL = "a text item must not carry a url";
    private static final String ERR_IMAGE_CONTENT = "an image item must not carry content";
    private static final String ERR_UNKNOWN_FIELDS = "an unknown item carries neither content nor a url";

    /**
     * Canonical constructor. Prefer the {@link #text(String)} / {@link #image(String)}
     * factories; this guards the type&#8596;field invariant so no construction path can
     * produce a cross-contaminated item (a text item with a url, an image item with
     * content, or an unknown item with either). A text item may legitimately carry a
     * {@code null} content on the read path when the server omits it, so that is allowed.
     */
    public DescriptionItem {
        Objects.requireNonNull(type, "type");
        if (type == DescriptionItemType.TEXT && url != null) {
            throw new IllegalArgumentException(ERR_TEXT_URL);
        }
        if (type == DescriptionItemType.IMAGE && content != null) {
            throw new IllegalArgumentException(ERR_IMAGE_CONTENT);
        }
        if (type == DescriptionItemType.UNKNOWN && (content != null || url != null)) {
            throw new IllegalArgumentException(ERR_UNKNOWN_FIELDS);
        }
    }

    /** A formatted-text item carrying the given HTML {@code content}. */
    public static DescriptionItem text(String content) {
        return new DescriptionItem(DescriptionItemType.TEXT, Objects.requireNonNull(content, ERR_CONTENT), null);
    }

    /** An image item referencing the given {@code url}. */
    public static DescriptionItem image(String url) {
        return new DescriptionItem(DescriptionItemType.IMAGE, null, Objects.requireNonNull(url, ERR_URL));
    }

    /**
     * Project a generated description item onto the consumer value.
     *
     * @param raw the generated item (a text/image subtype, or the base type if Allegro
     *            returned an item kind this release does not model — see C4)
     * @return the mapped item
     */
    public static DescriptionItem from(DescriptionSectionItemRaw raw) {
        if (raw instanceof DescriptionSectionItemTextRaw textRaw) {
            return new DescriptionItem(DescriptionItemType.TEXT, textRaw.getContent(), null);
        }
        if (raw instanceof DescriptionSectionItemImageRaw imageRaw) {
            return new DescriptionItem(DescriptionItemType.IMAGE, null, imageRaw.getUrl());
        }
        return new DescriptionItem(DescriptionItemType.UNKNOWN, null, null);
    }

    /**
     * The generated item for this value.
     *
     * @return the wire item (a text or image subtype)
     * @throws IllegalStateException on an {@link DescriptionItemType#UNKNOWN} item (not writable)
     */
    public DescriptionSectionItemRaw toRaw() {
        return switch (type) {
            case TEXT -> {
                DescriptionSectionItemTextRaw textRaw = new DescriptionSectionItemTextRaw();
                textRaw.setType(DescriptionItemType.WIRE_TEXT);
                textRaw.setContent(content);
                yield textRaw;
            }
            case IMAGE -> {
                DescriptionSectionItemImageRaw imageRaw = new DescriptionSectionItemImageRaw();
                imageRaw.setType(DescriptionItemType.WIRE_IMAGE);
                imageRaw.setUrl(url);
                yield imageRaw;
            }
            case UNKNOWN -> throw new IllegalStateException(ERR_NOT_WRITABLE);
        };
    }
}
