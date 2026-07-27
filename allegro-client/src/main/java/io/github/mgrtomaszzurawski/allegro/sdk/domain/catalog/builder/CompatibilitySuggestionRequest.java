/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder;

import org.jspecify.annotations.Nullable;

/**
 * The target of a compatibility-list suggestion
 * ({@code catalog().compatibility().suggestionsFor(...)}): exactly one of an offer
 * or a product to suggest a list for.
 *
 * <p>An offer id and a product id are mutually exclusive — a request must carry
 * exactly one, and setting neither or both is rejected fail-fast.
 * {@link Builder#language(String) language} localizes the item labels.
 *
 * <pre>{@code
 * CompatibilityList suggested =
 *         catalog.compatibility().suggestionsFor(CompatibilitySuggestionRequest.forOffer("12345"));
 * }</pre>
 *
 * @since 0.2.0
 */
public final class CompatibilitySuggestionRequest {

    private static final String ERR_NOT_EXACTLY_ONE =
            "a compatibility suggestion requires exactly one of offerId or productId";

    private final @Nullable String offerId;
    private final @Nullable String productId;
    private final @Nullable String language;

    private CompatibilitySuggestionRequest(Builder builder) {
        this.offerId = builder.offerId;
        this.productId = builder.productId;
        this.language = builder.language;
    }

    /** The offer to suggest a compatibility list for, or {@code null} when targeting a product. */
    public @Nullable String offerId() {
        return offerId;
    }

    /** The product to suggest a compatibility list for, or {@code null} when targeting an offer. */
    public @Nullable String productId() {
        return productId;
    }

    /** The language the item labels are localized to, or {@code null} for the default. */
    public @Nullable String language() {
        return language;
    }

    /** A suggestion for an offer (all other fields default). */
    public static CompatibilitySuggestionRequest forOffer(String offerId) {
        return builder().offerId(offerId).build();
    }

    /** A suggestion for a product (all other fields default). */
    public static CompatibilitySuggestionRequest forProduct(String productId) {
        return builder().productId(productId).build();
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this request. */
    public Builder toBuilder() {
        return new Builder().offerId(offerId).productId(productId).language(language);
    }

    /** Fluent builder for {@link CompatibilitySuggestionRequest}. */
    public static final class Builder {

        private @Nullable String offerId;
        private @Nullable String productId;
        private @Nullable String language;

        /** Target the offer with this id (mutually exclusive with a product). */
        public Builder offerId(@Nullable String offerId) {
            this.offerId = offerId;
            return this;
        }

        /** Target the product with this id (mutually exclusive with an offer). */
        public Builder productId(@Nullable String productId) {
            this.productId = productId;
            return this;
        }

        /** Localize the item labels to this language (e.g. {@code en-US}). */
        public Builder language(@Nullable String language) {
            this.language = language;
            return this;
        }

        /**
         * Build the request.
         *
         * @throws IllegalStateException if not exactly one of {@code offerId} /
         *     {@code productId} is set
         */
        public CompatibilitySuggestionRequest build() {
            if (isBlank(offerId) == isBlank(productId)) {
                throw new IllegalStateException(ERR_NOT_EXACTLY_ONE);
            }
            return new CompatibilitySuggestionRequest(this);
        }

        private static boolean isBlank(@Nullable String value) {
            return value == null || value.isBlank();
        }
    }
}
