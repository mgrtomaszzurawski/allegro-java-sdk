/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import io.github.mgrtomaszzurawski.allegro.client.model.StockSellingStatsRaw;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/**
 * Recent selling velocity for a fulfilled product, used to judge whether the
 * on-hand reserve is healthy.
 *
 * @param lastFourteenDaysAverage average units sold per day over the last 14 days
 * @param lastThirtyDaysSum       total units sold over the last 30 days
 *
 * @since 0.3.0
 */
public record StockSellingStats(
        @Nullable BigDecimal lastFourteenDaysAverage,
        @Nullable BigDecimal lastThirtyDaysSum) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static StockSellingStats from(StockSellingStatsRaw raw) {
        return new StockSellingStats(raw.getLastFourteenDaysAverage(), raw.getLastThirtyDaysSum());
    }
}
