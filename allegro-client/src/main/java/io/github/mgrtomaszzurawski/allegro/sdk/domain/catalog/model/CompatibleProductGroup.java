/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CompatibleProductsGroupsDtoGroupsInnerRaw;
import org.jspecify.annotations.Nullable;

/**
 * A group of compatible products — the coarse dimension (e.g. a vehicle make or
 * model family) whose {@link #id()} narrows a
 * {@code compatibility().products(...)} search via its group filter.
 *
 * @param id the group identifier (pass it as the group filter of a compatible-products search)
 * @param text the human-readable group name, or {@code null} when absent
 *
 * @since 0.2.0
 */
public record CompatibleProductGroup(String id, @Nullable String text) {

    /** Map one generated Layer-1 compatible-product group to the public record. */
    public static CompatibleProductGroup from(CompatibleProductsGroupsDtoGroupsInnerRaw raw) {
        return new CompatibleProductGroup(raw.getId(), raw.getText());
    }
}
