/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ShippingRateDeliveryMethodRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ShippingRateFirstItemRateRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ShippingRateMaxPackageWeightRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ShippingRateNextItemRateRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ShippingRateRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ShippingRateShippingTimeRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder.ShippingRateBuilder;
import org.jspecify.annotations.Nullable;

/**
 * One row of a shipping-rate set: it prices a single {@code deliveryMethodId} —
 * the flat rate for the first item, the incremental rate per further item, and
 * optional package-weight / quantity / dispatch-time constraints.
 *
 * @param deliveryMethodId the delivery method this row prices (see
 *     {@code shipping.deliveryMethods()})
 * @param firstItemRate the charge for the first item; {@code null} only if the
 *     server sends the (spec-required) rate object without an amount
 * @param nextItemRate the charge for each further item; {@code null} only if the
 *     server sends the (spec-required) rate object without an amount
 * @param maxQuantityPerPackage the most items a single package may hold, or {@code null}
 * @param maxPackageWeight the heaviest a single package may be, or {@code null}
 * @param shippingTime the promised dispatch-time range, or {@code null}
 *
 * @since 0.3.0
 */
public record ShippingRate(
        String deliveryMethodId,
        @Nullable Money firstItemRate,
        @Nullable Money nextItemRate,
        @Nullable Integer maxQuantityPerPackage,
        @Nullable Weight maxPackageWeight,
        @Nullable ShippingTime shippingTime) {

    /** A fresh builder for a {@link ShippingRate}. */
    public static ShippingRateBuilder builder() {
        return new ShippingRateBuilder();
    }

    /** A builder pre-loaded with this rate's fields. */
    public ShippingRateBuilder toBuilder() {
        return new ShippingRateBuilder()
                .deliveryMethodId(deliveryMethodId)
                .firstItemRate(firstItemRate)
                .nextItemRate(nextItemRate)
                .maxQuantityPerPackage(maxQuantityPerPackage)
                .maxPackageWeight(maxPackageWeight)
                .shippingTime(shippingTime);
    }

    /**
     * Map the generated Layer-1 DTO to the public immutable record. The delivery
     * method and the first/next-item rates are spec-required, so they are read
     * without a defensive null check; the weight, quantity and dispatch-time
     * constraints are optional.
     */
    public static ShippingRate from(ShippingRateRaw raw) {
        ShippingRateFirstItemRateRaw first = raw.getFirstItemRate();
        ShippingRateNextItemRateRaw next = raw.getNextItemRate();
        return new ShippingRate(
                raw.getDeliveryMethod().getId(),
                money(first.getAmount(), first.getCurrency()),
                money(next.getAmount(), next.getCurrency()),
                raw.getMaxQuantityPerPackage(),
                weight(raw.getMaxPackageWeight()),
                shippingTime(raw.getShippingTime()));
    }

    /** Build the generated Layer-1 DTO for the request body. */
    public ShippingRateRaw toRaw() {
        ShippingRateRaw raw = new ShippingRateRaw();
        ShippingRateDeliveryMethodRaw method = new ShippingRateDeliveryMethodRaw();
        method.setId(deliveryMethodId);
        raw.setDeliveryMethod(method);
        if (firstItemRate != null) {
            ShippingRateFirstItemRateRaw first = new ShippingRateFirstItemRateRaw();
            first.setAmount(firstItemRate.amount());
            first.setCurrency(firstItemRate.currency());
            raw.setFirstItemRate(first);
        }
        if (nextItemRate != null) {
            ShippingRateNextItemRateRaw next = new ShippingRateNextItemRateRaw();
            next.setAmount(nextItemRate.amount());
            next.setCurrency(nextItemRate.currency());
            raw.setNextItemRate(next);
        }
        if (maxQuantityPerPackage != null) {
            raw.setMaxQuantityPerPackage(maxQuantityPerPackage);
        }
        if (maxPackageWeight != null) {
            ShippingRateMaxPackageWeightRaw weight = new ShippingRateMaxPackageWeightRaw();
            weight.setValue(maxPackageWeight.value());
            weight.setUnit(maxPackageWeight.unit());
            raw.setMaxPackageWeight(weight);
        }
        if (shippingTime != null) {
            ShippingRateShippingTimeRaw time = new ShippingRateShippingTimeRaw();
            time.setFrom(shippingTime.fromTime());
            time.setTo(shippingTime.toTime());
            raw.setShippingTime(time);
        }
        return raw;
    }

    private static @Nullable Money money(@Nullable String amount, @Nullable String currency) {
        return amount == null || currency == null ? null : Money.of(amount, currency);
    }

    private static @Nullable Weight weight(@Nullable ShippingRateMaxPackageWeightRaw raw) {
        if (raw == null || raw.getValue() == null || raw.getUnit() == null) {
            return null;
        }
        return new Weight(raw.getValue(), raw.getUnit());
    }

    private static @Nullable ShippingTime shippingTime(@Nullable ShippingRateShippingTimeRaw raw) {
        if (raw == null || raw.getFrom() == null || raw.getTo() == null) {
            return null;
        }
        return new ShippingTime(raw.getFrom(), raw.getTo());
    }
}
