/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder;

import org.jspecify.annotations.Nullable;

/**
 * The criteria for a compatible-products search
 * ({@code catalog().compatibility().products(...)}).
 *
 * <p>A {@link Builder#type(String) type} is required — it names the kind of
 * compatible products, one of the values a category advertises as its
 * {@code itemsType} in {@code compatibility().supportedCategories()} (e.g.
 * {@code CAR}). The search must ALSO be narrowed by at least one of a
 * {@link Builder#groupId(String) group id} (from
 * {@code compatibility().productGroups(...)}), a TecDoc
 * {@link Builder#tecdocKTypNr(String) passenger} or
 * {@link Builder#tecdocNTypNr(String) commercial} vehicle number, or a free-text
 * {@link Builder#phrase(String) phrase} — Allegro rejects a type-only query
 * ({@code group.id is required}), so the builder enforces this fail-fast. When a
 * phrase is set, Allegro returns all matches at once (offset/limit are ignored), so
 * the returned stream is a single page.
 *
 * @since 0.2.0
 */
public final class CompatibleProductsFilter {

    private static final String ERR_NO_TYPE =
            "a compatible-products search requires a type (see supportedCategories() itemsType)";
    private static final String ERR_NO_NARROWING =
            "a compatible-products search requires at least one of groupId, tecdocKTypNr, "
                    + "tecdocNTypNr or phrase (Allegro rejects a type-only query)";

    private final String type;
    private final @Nullable String groupId;
    private final @Nullable String tecdocKTypNr;
    private final @Nullable String tecdocNTypNr;
    private final @Nullable String phrase;

    private CompatibleProductsFilter(Builder builder) {
        this.type = builder.type;
        this.groupId = builder.groupId;
        this.tecdocKTypNr = builder.tecdocKTypNr;
        this.tecdocNTypNr = builder.tecdocNTypNr;
        this.phrase = builder.phrase;
    }

    /** The compatible-products type; always set on a valid filter. */
    public String type() {
        return type;
    }

    /** The group to narrow to, or {@code null}. */
    public @Nullable String groupId() {
        return groupId;
    }

    /** The TecDoc passenger-vehicle number (kTypNr) to narrow to, or {@code null}. */
    public @Nullable String tecdocKTypNr() {
        return tecdocKTypNr;
    }

    /** The TecDoc commercial-vehicle number (nTypNr) to narrow to, or {@code null}. */
    public @Nullable String tecdocNTypNr() {
        return tecdocNTypNr;
    }

    /** The free-text phrase to match, or {@code null}. */
    public @Nullable String phrase() {
        return phrase;
    }

    /** A filter narrowed to a product group. */
    public static CompatibleProductsFilter inGroup(String type, String groupId) {
        return builder().type(type).groupId(groupId).build();
    }

    /** A filter narrowed by a free-text phrase (returns a single page). */
    public static CompatibleProductsFilter matching(String type, String phrase) {
        return builder().type(type).phrase(phrase).build();
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this filter. */
    public Builder toBuilder() {
        return new Builder().type(type).groupId(groupId)
                .tecdocKTypNr(tecdocKTypNr).tecdocNTypNr(tecdocNTypNr).phrase(phrase);
    }

    /** Fluent builder for {@link CompatibleProductsFilter}. */
    public static final class Builder {

        private @Nullable String type;
        private @Nullable String groupId;
        private @Nullable String tecdocKTypNr;
        private @Nullable String tecdocNTypNr;
        private @Nullable String phrase;

        /** The compatible-products type (a {@code supportedCategories()} {@code itemsType}). */
        public Builder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        /** Narrow to a product group (id from {@code productGroups(...)}). */
        public Builder groupId(@Nullable String groupId) {
            this.groupId = groupId;
            return this;
        }

        /** Narrow to a TecDoc passenger vehicle (kTypNr). */
        public Builder tecdocKTypNr(@Nullable String tecdocKTypNr) {
            this.tecdocKTypNr = tecdocKTypNr;
            return this;
        }

        /** Narrow to a TecDoc commercial vehicle (nTypNr). */
        public Builder tecdocNTypNr(@Nullable String tecdocNTypNr) {
            this.tecdocNTypNr = tecdocNTypNr;
            return this;
        }

        /** Match by free-text phrase (offset/limit are ignored when set). */
        public Builder phrase(@Nullable String phrase) {
            this.phrase = phrase;
            return this;
        }

        /**
         * Build the filter.
         *
         * @throws IllegalStateException if no type is set, or if none of
         *     {@code groupId} / {@code tecdocKTypNr} / {@code tecdocNTypNr} /
         *     {@code phrase} is set (Allegro rejects a type-only query)
         */
        public CompatibleProductsFilter build() {
            if (isBlank(type)) {
                throw new IllegalStateException(ERR_NO_TYPE);
            }
            if (isBlank(groupId) && isBlank(tecdocKTypNr) && isBlank(tecdocNTypNr) && isBlank(phrase)) {
                throw new IllegalStateException(ERR_NO_NARROWING);
            }
            return new CompatibleProductsFilter(this);
        }

        private static boolean isBlank(@Nullable String value) {
            return value == null || value.isBlank();
        }
    }
}
