/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * The selling format a {@link OfferFeePreviewRequest} previews fees for. The sale
 * commission depends on how the offer is sold, so the fee preview carries the
 * format and its price rather than a bare amount.
 *
 * <p>Create a mode with the static factories: {@link #buyNow(Money)} (optionally
 * with a net price) for a fixed-price offer, or {@link #auction(Money)}
 * (optionally with a minimal price) for an auction.
 *
 * @since 0.1.0
 */
public sealed interface FeePreviewSellingMode
        permits FeePreviewSellingMode.BuyNow, FeePreviewSellingMode.Auction {

    /**
     * A fixed-price (Buy Now) offer sold at {@code price}.
     *
     * @param price the Buy Now price
     * @return a Buy Now selling mode
     */
    static BuyNow buyNow(Money price) {
        return new BuyNow(price, null);
    }

    /**
     * A fixed-price (Buy Now) offer sold at {@code price}, with a declared net
     * price for a seller that prices net of VAT.
     *
     * @param price the gross Buy Now price
     * @param netPrice the net price
     * @return a Buy Now selling mode carrying a net price
     */
    static BuyNow buyNow(Money price, Money netPrice) {
        return new BuyNow(price, Objects.requireNonNull(netPrice, "netPrice"));
    }

    /**
     * An auction opening at {@code startingPrice}.
     *
     * @param startingPrice the auction starting price
     * @return an auction selling mode
     */
    static Auction auction(Money startingPrice) {
        return new Auction(startingPrice, null);
    }

    /**
     * An auction opening at {@code startingPrice} with a reserve
     * ({@code minimalPrice}) below which the item is not sold.
     *
     * @param startingPrice the auction starting price
     * @param minimalPrice the reserve price
     * @return an auction selling mode carrying a reserve price
     */
    static Auction auction(Money startingPrice, Money minimalPrice) {
        return new Auction(startingPrice, Objects.requireNonNull(minimalPrice, "minimalPrice"));
    }

    /**
     * A fixed-price (Buy Now) selling mode.
     *
     * @param price the Buy Now price (required)
     * @param netPrice the net price, or {@code null} when the seller prices gross
     */
    record BuyNow(Money price, @Nullable Money netPrice) implements FeePreviewSellingMode {

        /** Compact constructor validating the required price. */
        public BuyNow {
            Objects.requireNonNull(price, "price");
        }
    }

    /**
     * An auction selling mode.
     *
     * @param startingPrice the starting price (required)
     * @param minimalPrice the reserve price, or {@code null} for no reserve
     */
    record Auction(Money startingPrice, @Nullable Money minimalPrice) implements FeePreviewSellingMode {

        /** Compact constructor validating the required starting price. */
        public Auction {
            Objects.requireNonNull(startingPrice, "startingPrice");
        }
    }
}
