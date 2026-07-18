/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.AfterSalesServices;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferDelivery;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A request to create a new Buy Now offer. The essential fields are required and
 * validated fail-fast at {@link Builder#build()}; the created offer starts as a
 * draft (publish it with {@code offers().batch().publish(...)}).
 *
 * <pre>{@code
 * CreateOfferRequest request = CreateOfferRequest.builder()
 *         .name("Mechanical keyboard")
 *         .categoryId("257")
 *         .buyNowPrice(Money.of("199.99", "PLN"))
 *         .availableStock(10)
 *         .build();
 * }</pre>
 *
 * @since 0.2.0
 */
public final class CreateOfferRequest {

    private static final String ERR_NAME = "name is required";
    private static final String ERR_CATEGORY = "categoryId is required";
    private static final String ERR_PRICE = "buyNowPrice is required";
    private static final String ERR_STOCK = "availableStock is required and must not be negative";

    private final String name;
    private final String categoryId;
    private final Money buyNowPrice;
    private final int availableStock;
    private final List<String> imageUrls;
    private final @Nullable OfferDelivery delivery;
    private final @Nullable AfterSalesServices afterSalesServices;

    private CreateOfferRequest(Builder builder) {
        this.name = builder.name;
        this.categoryId = builder.categoryId;
        this.buyNowPrice = builder.buyNowPrice;
        this.availableStock = builder.availableStock;
        this.imageUrls = List.copyOf(builder.imageUrls);
        this.delivery = builder.delivery;
        this.afterSalesServices = builder.afterSalesServices;
    }

    /** The offer title. */
    public String name() {
        return name;
    }

    /** The Allegro category the offer is listed in. */
    public String categoryId() {
        return categoryId;
    }

    /** The fixed Buy Now price. */
    public Money buyNowPrice() {
        return buyNowPrice;
    }

    /** The available quantity. */
    public int availableStock() {
        return availableStock;
    }

    /** Image URLs, in display order (possibly empty). */
    public List<String> imageUrls() {
        return imageUrls;
    }

    /** The offer's delivery terms, or {@code null} if not set. */
    public @Nullable OfferDelivery delivery() {
        return delivery;
    }

    /** The offer's after-sales conditions, or {@code null} if not set. */
    public @Nullable AfterSalesServices afterSalesServices() {
        return afterSalesServices;
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder for {@link CreateOfferRequest}. */
    public static final class Builder {

        private @Nullable String name;
        private @Nullable String categoryId;
        private @Nullable Money buyNowPrice;
        private @Nullable Integer availableStock;
        private List<String> imageUrls = List.of();
        private @Nullable OfferDelivery delivery;
        private @Nullable AfterSalesServices afterSalesServices;

        /** The offer title (required). */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /** The Allegro category id (required). */
        public Builder categoryId(String categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        /** The fixed Buy Now price (required). */
        public Builder buyNowPrice(Money buyNowPrice) {
            this.buyNowPrice = buyNowPrice;
            return this;
        }

        /** The available quantity (required, non-negative). */
        public Builder availableStock(int availableStock) {
            this.availableStock = availableStock;
            return this;
        }

        /** Image URLs, in display order (optional). */
        public Builder imageUrls(List<String> imageUrls) {
            this.imageUrls = List.copyOf(imageUrls);
            return this;
        }

        /** Set the offer's delivery terms (optional). */
        public Builder delivery(@Nullable OfferDelivery delivery) {
            this.delivery = delivery;
            return this;
        }

        /** Set the offer's after-sales conditions (optional). */
        public Builder afterSalesServices(@Nullable AfterSalesServices afterSalesServices) {
            this.afterSalesServices = afterSalesServices;
            return this;
        }

        /** Validate the required fields and build; throws {@link IllegalStateException} if any is missing. */
        public CreateOfferRequest build() {
            if (name == null) {
                throw new IllegalStateException(ERR_NAME);
            }
            if (categoryId == null) {
                throw new IllegalStateException(ERR_CATEGORY);
            }
            if (buyNowPrice == null) {
                throw new IllegalStateException(ERR_PRICE);
            }
            if (availableStock == null || availableStock < 0) {
                throw new IllegalStateException(ERR_STOCK);
            }
            return new CreateOfferRequest(this);
        }
    }
}
