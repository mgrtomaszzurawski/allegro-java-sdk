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
 * {@code CAR}); the groups of that type are returned lazily, offset-paginated. The
 * groups endpoint takes no other filter — unlike
 * {@link CompatibleProductsFilter products}, it has no phrase or group/TecDoc
 * narrowing.
 *
 * @since 0.2.0
 */
public final class CompatibleProductGroupsFilter {

    private static final String ERR_NO_TYPE =
            "a compatible-product-groups search requires a type (see supportedCategories() itemsType)";

    private final String type;

    private CompatibleProductGroupsFilter(Builder builder) {
        this.type = builder.type;
    }

    /** The compatible-products type; always set on a valid filter. */
    public String type() {
        return type;
    }

    /** A filter for the groups of a type. */
    public static CompatibleProductGroupsFilter ofType(String type) {
        return builder().type(type).build();
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this filter. */
    public Builder toBuilder() {
        return new Builder().type(type);
    }

    /** Fluent builder for {@link CompatibleProductGroupsFilter}. */
    public static final class Builder {

        private @Nullable String type;

        /** The compatible-products type (a {@code supportedCategories()} {@code itemsType}). */
        public Builder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        /**
         * Build the filter.
         *
         * @throws IllegalStateException if no type is set
         */
        public CompatibleProductGroupsFilter build() {
            if (type == null || type.isBlank()) {
                throw new IllegalStateException(ERR_NO_TYPE);
            }
            return new CompatibleProductGroupsFilter(this);
        }
    }
}
