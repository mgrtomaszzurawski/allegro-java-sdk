/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A partial edit of an existing offer — only the fields you set are sent (the
 * edit is a PATCH). Leave a field unset to keep its current value.
 *
 * <pre>{@code
 * EditOfferRequest request = EditOfferRequest.builder()
 *         .name("Mechanical keyboard (2026 edition)")
 *         .availableStock(25)
 *         .build();
 * }</pre>
 *
 * @since 0.2.0
 */
public final class EditOfferRequest {

    private final @Nullable String name;
    private final @Nullable Money buyNowPrice;
    private final @Nullable Integer availableStock;
    private final @Nullable List<String> imageUrls;

    private EditOfferRequest(Builder builder) {
        this.name = builder.name;
        this.buyNowPrice = builder.buyNowPrice;
        this.availableStock = builder.availableStock;
        this.imageUrls = builder.imageUrls == null ? null : List.copyOf(builder.imageUrls);
    }

    /** The new title, or {@code null} to leave it unchanged. */
    public @Nullable String name() {
        return name;
    }

    /** The new Buy Now price, or {@code null} to leave it unchanged. */
    public @Nullable Money buyNowPrice() {
        return buyNowPrice;
    }

    /** The new available quantity, or {@code null} to leave it unchanged. */
    public @Nullable Integer availableStock() {
        return availableStock;
    }

    /** The new image URLs, or {@code null} to leave them unchanged. */
    public @Nullable List<String> imageUrls() {
        return imageUrls;
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder for {@link EditOfferRequest}. */
    public static final class Builder {

        private @Nullable String name;
        private @Nullable Money buyNowPrice;
        private @Nullable Integer availableStock;
        private @Nullable List<String> imageUrls;

        /** Change the title. */
        public Builder name(@Nullable String name) {
            this.name = name;
            return this;
        }

        /** Change the Buy Now price. */
        public Builder buyNowPrice(@Nullable Money buyNowPrice) {
            this.buyNowPrice = buyNowPrice;
            return this;
        }

        /** Change the available quantity. */
        public Builder availableStock(@Nullable Integer availableStock) {
            this.availableStock = availableStock;
            return this;
        }

        /** Change the image URLs. */
        public Builder imageUrls(@Nullable List<String> imageUrls) {
            this.imageUrls = imageUrls == null ? null : List.copyOf(imageUrls);
            return this;
        }

        /** Build the partial edit. */
        public EditOfferRequest build() {
            return new EditOfferRequest(this);
        }
    }
}
