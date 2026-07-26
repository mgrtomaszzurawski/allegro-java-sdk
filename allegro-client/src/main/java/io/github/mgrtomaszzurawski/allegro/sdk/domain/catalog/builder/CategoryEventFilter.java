/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.CategoryEventType;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Filter for the category-change stream
 * ({@code catalog().categories().streamChanges(...)}).
 *
 * <p>{@link #from() from} resumes the stream after a previously seen event (pass the
 * last {@code CategoryEvent.id()} you processed; {@code null} starts from the
 * beginning). {@link #types() types} restricts the stream to certain change kinds
 * ({@code CATEGORY_CREATED}, {@code CATEGORY_MOVED}, …); empty streams every kind.
 *
 * @param from the event id to resume after, or {@code null} to start from the beginning
 * @param types the change kinds to include; empty for all
 *
 * @since 0.2.0
 */
public record CategoryEventFilter(@Nullable String from, List<CategoryEventType> types) {

    /** Canonical constructor; defensively copies {@code types} immutable. */
    public CategoryEventFilter {
        types = types == null ? List.of() : List.copyOf(types);
    }

    /** Every change kind, from the beginning of the feed. */
    public static CategoryEventFilter all() {
        return new CategoryEventFilter(null, List.of());
    }

    /** Every change kind, resuming after the given event id. */
    public static CategoryEventFilter since(String from) {
        return new CategoryEventFilter(Objects.requireNonNull(from, "from"), List.of());
    }

    /** The given change kinds, from the beginning of the feed. */
    public static CategoryEventFilter ofTypes(CategoryEventType... types) {
        return new CategoryEventFilter(null, List.of(types));
    }
}
