/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model;

import io.github.mgrtomaszzurawski.allegro.client.model.TurnoverDiscountThresholdDtoDiscountRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TurnoverDiscountThresholdDtoMinimumTurnoverRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TurnoverDiscountThresholdDtoRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.util.Objects;

/**
 * One turnover-discount threshold: once a buyer's cumulated turnover reaches
 * {@link #minimumTurnover()}, they receive {@link #discountPercentage()} off.
 * Used both when setting a discount and when reading one back.
 *
 * @param minimumTurnover the turnover a buyer must reach to unlock the discount
 * @param discountPercentage the discount as a decimal percentage string
 *     (e.g. {@code "5"}), the exact form Allegro expects and returns
 *
 * @since 0.3.0
 */
public record TurnoverThreshold(Money minimumTurnover, String discountPercentage) {

    private static final String ERR_MINIMUM_TURNOVER = "minimumTurnover must not be null";
    private static final String ERR_DISCOUNT_PERCENTAGE = "discountPercentage must not be null";

    /** Rejects a missing turnover threshold or discount percentage. */
    public TurnoverThreshold {
        Objects.requireNonNull(minimumTurnover, ERR_MINIMUM_TURNOVER);
        Objects.requireNonNull(discountPercentage, ERR_DISCOUNT_PERCENTAGE);
    }

    /**
     * Map the generated threshold DTO to the public record. The nested turnover
     * and discount are {@code nullable} in the spec, so each is null-checked with
     * a clear message before use rather than dereferenced blindly.
     *
     * @param raw the generated threshold DTO
     * @return the mapped record
     */
    public static TurnoverThreshold from(TurnoverDiscountThresholdDtoRaw raw) {
        TurnoverDiscountThresholdDtoMinimumTurnoverRaw minimumTurnover =
                Objects.requireNonNull(raw.getMinimumTurnover(), ERR_MINIMUM_TURNOVER);
        TurnoverDiscountThresholdDtoDiscountRaw discount =
                Objects.requireNonNull(raw.getDiscount(), ERR_DISCOUNT_PERCENTAGE);
        return new TurnoverThreshold(
                Money.of(minimumTurnover.getAmount(), minimumTurnover.getCurrency()),
                discount.getPercentage());
    }
}
