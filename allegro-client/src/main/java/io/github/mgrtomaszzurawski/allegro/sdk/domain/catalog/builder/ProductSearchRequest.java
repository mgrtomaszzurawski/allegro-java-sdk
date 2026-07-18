/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder;

import org.jspecify.annotations.Nullable;

/**
 * The criteria for a product search ({@code catalog().products().search(...)}).
 *
 * <p>A {@link Builder#phrase(String) phrase} is required — a search without one
 * is rejected fail-fast. {@link Builder#categoryId(String) categoryId} is an
 * optional filter that Allegro only honours <em>together with</em> a phrase
 * (per the spec, "the category identifier ... can only be used when searching by
 * phrase"). {@link Builder#language(String) language} localizes the matched
 * product names. Pagination is handled internally by the returned lazy stream
 * (the opaque {@code page.id} cursor is threaded automatically).
 *
 * <pre>{@code
 * catalog.products()
 *         .search(ProductSearchRequest.builder().phrase("iphone 15").categoryId("257").build())
 *         .limit(50)
 *         .forEach(summary -> System.out.println(summary.name()));
 * }</pre>
 *
 * @since 0.2.0
 */
public final class ProductSearchRequest {

    private static final String ERR_NO_PHRASE =
            "a product search requires a phrase (categoryId only filters a phrase search)";

    private final @Nullable String phrase;
    private final @Nullable String categoryId;
    private final @Nullable String language;

    private ProductSearchRequest(Builder builder) {
        this.phrase = builder.phrase;
        this.categoryId = builder.categoryId;
        this.language = builder.language;
    }

    /** The free-text phrase to match; always set on a valid request. */
    public @Nullable String phrase() {
        return phrase;
    }

    /** The category the phrase search is restricted to, or {@code null} for all categories. */
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

        /** Restrict the phrase search to this category (only honoured with a phrase). */
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
         * @throws IllegalStateException if no phrase is set (a category alone is
         *     not a valid search)
         */
        public ProductSearchRequest build() {
            if (isBlank(phrase)) {
                throw new IllegalStateException(ERR_NO_PHRASE);
            }
            return new ProductSearchRequest(this);
        }

        private static boolean isBlank(@Nullable String value) {
            return value == null || value.isBlank();
        }
    }
}
