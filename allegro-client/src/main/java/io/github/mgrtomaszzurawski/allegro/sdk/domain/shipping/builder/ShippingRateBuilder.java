/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRate;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingTime;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.Weight;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for one {@link ShippingRate} row. The delivery method, the
 * maximum quantity per package, and the first- and next-item rates are required;
 * the package-weight limit and dispatch-time range are optional.
 *
 * @since 0.3.0
 */
public final class ShippingRateBuilder {

    private static final String FIELD_DELIVERY_METHOD_ID = "ShippingRate.deliveryMethodId";
    private static final String FIELD_MAX_QUANTITY = "ShippingRate.maxQuantityPerPackage";
    private static final String FIELD_FIRST_ITEM_RATE = "ShippingRate.firstItemRate";
    private static final String FIELD_NEXT_ITEM_RATE = "ShippingRate.nextItemRate";

    private @Nullable String deliveryMethodId;
    private @Nullable Money firstItemRate;
    private @Nullable Money nextItemRate;
    private @Nullable Integer maxQuantityPerPackage;
    private @Nullable Weight maxPackageWeight;
    private @Nullable ShippingTime shippingTime;

    /** The delivery method this row prices (required; see {@code deliveryMethods()}). */
    public ShippingRateBuilder deliveryMethodId(@Nullable String value) {
        this.deliveryMethodId = value;
        return this;
    }

    /** The charge for the first item (required). */
    public ShippingRateBuilder firstItemRate(@Nullable Money value) {
        this.firstItemRate = value;
        return this;
    }

    /** The charge for each further item (required). */
    public ShippingRateBuilder nextItemRate(@Nullable Money value) {
        this.nextItemRate = value;
        return this;
    }

    /** The most items a single package may hold (required). */
    public ShippingRateBuilder maxQuantityPerPackage(@Nullable Integer value) {
        this.maxQuantityPerPackage = value;
        return this;
    }

    /** The heaviest a single package may be (optional). */
    public ShippingRateBuilder maxPackageWeight(@Nullable Weight value) {
        this.maxPackageWeight = value;
        return this;
    }

    /** The promised dispatch-time range (optional). */
    public ShippingRateBuilder shippingTime(@Nullable ShippingTime value) {
        this.shippingTime = value;
        return this;
    }

    /**
     * Validate and assemble the immutable {@link ShippingRate}.
     *
     * @throws IllegalStateException if a required field is missing
     */
    public ShippingRate build() {
        String validDeliveryMethodId =
                BuilderValidation.requireText(deliveryMethodId, FIELD_DELIVERY_METHOD_ID);
        Money validFirstItemRate =
                BuilderValidation.requirePresent(firstItemRate, FIELD_FIRST_ITEM_RATE);
        Money validNextItemRate =
                BuilderValidation.requirePresent(nextItemRate, FIELD_NEXT_ITEM_RATE);
        Integer validMaxQuantity =
                BuilderValidation.requirePresent(maxQuantityPerPackage, FIELD_MAX_QUANTITY);
        return new ShippingRate(validDeliveryMethodId, validFirstItemRate, validNextItemRate,
                validMaxQuantity, maxPackageWeight, shippingTime);
    }
}
