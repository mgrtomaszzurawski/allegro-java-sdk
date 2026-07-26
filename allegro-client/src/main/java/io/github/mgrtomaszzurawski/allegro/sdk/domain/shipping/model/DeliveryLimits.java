/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.DeliveryOptionDtoLimitsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MoneyDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.WeightValueRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import org.jspecify.annotations.Nullable;

/**
 * The upper limits a delivery option imposes on a shipment: the maximum
 * cash-on-delivery and insurance amounts, the maximum package dimensions and
 * the maximum package weight. Any limit the server omits is {@code null}.
 * Read-only: it appears only in a delivery proposal's options.
 *
 * @param cashOnDelivery the maximum cash-on-delivery amount, or {@code null}
 * @param insurance the maximum insurance amount, or {@code null}
 * @param dimensions the maximum package dimensions, or {@code null}
 * @param weight the maximum package weight, or {@code null}
 *
 * @since 0.5.0
 */
public record DeliveryLimits(
        @Nullable Money cashOnDelivery,
        @Nullable Money insurance,
        @Nullable PackageDimensions dimensions,
        @Nullable Weight weight) {

    /** Map the generated limits DTO, or {@code null} when absent. */
    public static @Nullable DeliveryLimits from(@Nullable DeliveryOptionDtoLimitsRaw raw) {
        if (raw == null) {
            return null;
        }
        return new DeliveryLimits(
                money(raw.getCashOnDelivery()),
                money(raw.getInsurance()),
                PackageDimensions.from(raw.getDimensions()),
                weight(raw.getWeight()));
    }

    private static @Nullable Money money(@Nullable MoneyDtoRaw raw) {
        if (raw == null || raw.getAmount() == null || raw.getCurrency() == null) {
            return null;
        }
        return Money.of(raw.getAmount(), raw.getCurrency());
    }

    private static @Nullable Weight weight(@Nullable WeightValueRaw raw) {
        if (raw == null || raw.getValue() == null || raw.getUnit() == null) {
            return null;
        }
        return new Weight(raw.getValue().toPlainString(), raw.getUnit().getValue());
    }
}
