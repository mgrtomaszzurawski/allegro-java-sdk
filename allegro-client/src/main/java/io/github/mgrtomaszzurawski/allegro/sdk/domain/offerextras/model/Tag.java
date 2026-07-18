/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model;

import io.github.mgrtomaszzurawski.allegro.client.model.TagListResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TagResponseRaw;
import java.util.List;

/**
 * A seller's offer tag.
 *
 * @param id the tag identifier
 * @param name the tag name
 * @param hidden whether the tag is hidden from the seller's own tag views
 *
 * @since 0.2.0
 */
public record Tag(String id, String name, boolean hidden) {

    /** Map one generated Layer-1 tag DTO to the public record. */
    static Tag from(TagResponseRaw raw) {
        return new Tag(raw.getId(), raw.getName(), raw.getHidden());
    }

    /** Map the generated Layer-1 list response to public records. */
    public static List<Tag> listFrom(TagListResponseRaw raw) {
        return raw.getTags() == null ? List.of() : raw.getTags().stream().map(Tag::from).toList();
    }
}
