/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder;

import org.jspecify.annotations.Nullable;

/**
 * The criteria for a product search ({@code catalog().products().search(...)}).
 *
 * <p>Supply a {@link Builder#phrase(String) phrase}, a
 * {@link Builder#categoryId(String) categoryId}, or both — a search with neither
 * is rejected fail-fast, because the endpoint needs at least one criterion.
 * {@link Builder#language(String) language} localizes the matched product names.
 * Pagination is handled internally by the returned lazy stream (the opaque
 * {@code page.id} cursor is threaded automatically).
 *
 * <pre>{@code
 * catalog.products()
 *         .search(ProductSearchRequest.builder().phrase("iphone 15").build())
 *         .limit(50)
 *         .forEach(summary -> System.out.println(summary.name()));
 * }</pre>
 *
 * @since 0.2.0
 */
public final class ProductSearchRequest {

    private static final String ERR_NO_CRITERIA =
            "a product search needs at least a phrase or a categoryId";

    private final @Nullable String phrase;
    private final @Nullable String categoryId;
    private final @Nullable String language;

    private ProductSearchRequest(Builder builder) {
        this.phrase = builder.phrase;
        this.categoryId = builder.categoryId;
        this.language = builder.language;
    }

    /** The free-text phrase to match, or {@code null} when searching by category alone. */
    public @Nullable String phrase() {
        return phrase;
    }

    /** The category to restrict the search to, or {@code null}. */
    public @Nullable String categoryId() {
        return categoryId;
    }

    /** The language matched product names are localized to, or {@code null} for the default. */
    public @Nullable String language() {
        return language;
    }

    /** A search for a phrase across all categories. */
    public static ProductSearchRequest byPhrase(String phrase) {
        return builder().phrase(phrase).build();
    }

    /** A search restricted to one category. */
    public static ProductSearchRequest inCategory(String categoryId) {
        return builder().categoryId(categoryId).build();
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this request. */
    public Builder toBuilder() {
        return new Builder().phrase(phrase).categoryId(categoryId).language(language);
    }

    /** Fluent builder for {@link ProductSearchRequest}. */
    public static final class Builder {

        private @Nullable String phrase;
        private @Nullable String categoryId;
        private @Nullable String language;

        /** Match products by this free-text phrase. */
        public Builder phrase(@Nullable String phrase) {
            this.phrase = phrase;
            return this;
        }

        /** Restrict the search to this category. */
        public Builder categoryId(@Nullable String categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        /** Localize matched product names to this language (e.g. {@code en-US}). */
        public Builder language(@Nullable String language) {
            this.language = language;
            return this;
        }

        /**
         * Build the request.
         *
         * @throws IllegalStateException if neither a phrase nor a categoryId is set
         */
        public ProductSearchRequest build() {
            if (isBlank(phrase) && isBlank(categoryId)) {
                throw new IllegalStateException(ERR_NO_CRITERIA);
            }
            return new ProductSearchRequest(this);
        }

        private static boolean isBlank(@Nullable String value) {
            return value == null || value.isBlank();
        }
    }
}
