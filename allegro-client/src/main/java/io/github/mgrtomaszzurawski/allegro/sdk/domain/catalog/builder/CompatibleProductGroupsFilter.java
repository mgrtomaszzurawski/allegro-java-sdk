/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder;

import org.jspecify.annotations.Nullable;

/**
 * The criteria for a compatible-product-groups search
 * ({@code catalog().compatibility().productGroups(...)}).
 *
 * <p>A {@link Builder#type(String) type} is required (a value a category advertises
 * as its {@code itemsType} in {@code compatibility().supportedCategories()}, e.g.
 * {@code CAR}); an optional {@link Builder#phrase(String) phrase} narrows the
 * groups. When a phrase is set, Allegro returns all matches at once (offset/limit
 * are ignored), so the returned stream is a single page.
 *
 * @since 0.2.0
 */
public final class CompatibleProductGroupsFilter {

    private static final String ERR_NO_TYPE =
            "a compatible-product-groups search requires a type (see supportedCategories() itemsType)";

    private final String type;
    private final @Nullable String phrase;

    private CompatibleProductGroupsFilter(Builder builder) {
        this.type = builder.type;
        this.phrase = builder.phrase;
    }

    /** The compatible-products type; always set on a valid filter. */
    public String type() {
        return type;
    }

    /** The free-text phrase to match, or {@code null}. */
    public @Nullable String phrase() {
        return phrase;
    }

    /** A filter of just a type (phrase defaults). */
    public static CompatibleProductGroupsFilter ofType(String type) {
        return builder().type(type).build();
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this filter. */
    public Builder toBuilder() {
        return new Builder().type(type).phrase(phrase);
    }

    /** Fluent builder for {@link CompatibleProductGroupsFilter}. */
    public static final class Builder {

        private @Nullable String type;
        private @Nullable String phrase;

        /** The compatible-products type (a {@code supportedCategories()} {@code itemsType}). */
        public Builder type(@Nullable String type) {
            this.type = type;
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
         * @throws IllegalStateException if no type is set
         */
        public CompatibleProductGroupsFilter build() {
            if (isBlank(type)) {
                throw new IllegalStateException(ERR_NO_TYPE);
            }
            return new CompatibleProductGroupsFilter(this);
        }

        private static boolean isBlank(@Nullable String value) {
            return value == null || value.isBlank();
        }
    }
}
