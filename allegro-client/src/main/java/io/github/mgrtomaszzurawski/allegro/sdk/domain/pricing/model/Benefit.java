/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * The reward a rebate {@link Promotion} grants — a sealed hierarchy over the
 * benefit families Allegro models. Consumers match on the concrete type:
 * <pre>{@code
 * if (benefit instanceof Benefit.LargeOrderDiscount large) {
 *     large.thresholds().forEach(...);
 * }
 * }</pre>
 *
 * <p>Read responses may carry a {@link UnknownBenefit} when Allegro introduces a
 * benefit family this SDK version does not model yet (the polymorphic subtype
 * degrades to the base rather than failing the whole read); it cannot be sent
 * back on a write.
 *
 * @since 0.4.0
 */
public sealed interface Benefit
        permits Benefit.LargeOrderDiscount, Benefit.MultiPackDiscount,
                Benefit.WholesalePriceList, Benefit.UnknownBenefit {

    /**
     * One tier of a {@link LargeOrderDiscount}: from an order value up, apply a
     * percentage discount.
     *
     * @param orderValueFrom the inclusive lower bound of the order value the
     *     tier applies from
     * @param discountPercentage the discount as a decimal string (e.g.
     *     {@code "10"}), the exact form Allegro expects and returns
     * @since 0.4.0
     */
    record OrderValueThreshold(Money orderValueFrom, String discountPercentage) {

        private static final String ERR_ORDER_VALUE = "orderValueFrom must not be null";
        private static final String ERR_PERCENTAGE = "discountPercentage must not be null";

        /** Rejects a missing bound or percentage. */
        public OrderValueThreshold {
            Objects.requireNonNull(orderValueFrom, ERR_ORDER_VALUE);
            Objects.requireNonNull(discountPercentage, ERR_PERCENTAGE);
        }
    }

    /**
     * One tier of a {@link WholesalePriceList}: from a quantity up, apply a
     * percentage discount.
     *
     * @param quantityFrom the inclusive lower bound of the quantity the tier
     *     applies from
     * @param discountPercentage the discount as a decimal string (e.g.
     *     {@code "10"}), the exact form Allegro expects and returns
     * @since 0.4.0
     */
    record QuantityThreshold(BigDecimal quantityFrom, String discountPercentage) {

        private static final String ERR_QUANTITY = "quantityFrom must not be null";
        private static final String ERR_PERCENTAGE = "discountPercentage must not be null";

        /** Rejects a missing bound or percentage. */
        public QuantityThreshold {
            Objects.requireNonNull(quantityFrom, ERR_QUANTITY);
            Objects.requireNonNull(discountPercentage, ERR_PERCENTAGE);
        }
    }

    /**
     * A discount that grows in tiers with the total order value.
     *
     * @param thresholds the order-value tiers, ascending
     * @since 0.4.0
     */
    record LargeOrderDiscount(List<OrderValueThreshold> thresholds) implements Benefit {

        /** Defensively copies the tiers so the record stays immutable. */
        public LargeOrderDiscount {
            thresholds = List.copyOf(thresholds);
        }
    }

    /**
     * A multipack "buy several, get some discounted" benefit
     * ({@code UNIT_PERCENTAGE_DISCOUNT}): for every {@code buyQuantity} bought,
     * {@code discountedQuantity} of them get {@code discountPercentage} off.
     *
     * @param discountPercentage the discount as a decimal string (e.g.
     *     {@code "50"}), the exact form Allegro expects and returns
     * @param buyQuantity the quantity that must be bought to trigger the discount
     * @param discountedQuantity how many of the bought units are discounted
     * @since 0.4.0
     */
    record MultiPackDiscount(String discountPercentage, BigDecimal buyQuantity,
            BigDecimal discountedQuantity) implements Benefit {

        private static final String ERR_PERCENTAGE = "discountPercentage must not be null";
        private static final String ERR_BUY_QUANTITY = "buyQuantity must not be null";
        private static final String ERR_DISCOUNTED_QUANTITY = "discountedQuantity must not be null";

        /** Rejects any missing field. */
        public MultiPackDiscount {
            Objects.requireNonNull(discountPercentage, ERR_PERCENTAGE);
            Objects.requireNonNull(buyQuantity, ERR_BUY_QUANTITY);
            Objects.requireNonNull(discountedQuantity, ERR_DISCOUNTED_QUANTITY);
        }
    }

    /**
     * A named, quantity-tiered wholesale price list.
     *
     * @param name the price-list name shown to buyers
     * @param thresholds the quantity tiers, ascending
     * @since 0.4.0
     */
    record WholesalePriceList(String name, List<QuantityThreshold> thresholds) implements Benefit {

        private static final String ERR_NAME = "name must not be null";

        /** Rejects a missing name and defensively copies the tiers. */
        public WholesalePriceList {
            Objects.requireNonNull(name, ERR_NAME);
            thresholds = List.copyOf(thresholds);
        }
    }

    /**
     * A benefit family this SDK version does not model yet, surfaced on read so
     * an unrecognised type does not fail the whole response. Read-only: it cannot
     * be sent on a create or modify.
     *
     * @param type the wire discriminator Allegro returned
     * @since 0.4.0
     */
    record UnknownBenefit(String type) implements Benefit {

        private static final String ERR_TYPE = "type must not be null";

        /** Rejects a missing discriminator. */
        public UnknownBenefit {
            Objects.requireNonNull(type, ERR_TYPE);
        }
    }
}
