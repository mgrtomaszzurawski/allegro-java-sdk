/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model;

import io.github.mgrtomaszzurawski.allegro.client.model.SellerOfferStatsResponseDtoRaw;
import java.util.List;

/**
 * The advertisement statistics aggregated across all of the seller's
 * advertisements, as returned by {@code Classifieds.sellerStats(...)}: the
 * per-event totals over the requested period plus the day-by-day breakdown.
 *
 * @param totals the per-event-type totals over the period; never {@code null},
 *     possibly empty
 * @param perDay the day-by-day breakdown; never {@code null}, possibly empty
 *
 * @since 0.2.0
 */
public record SellerClassifiedStats(
        List<ClassifiedEventStat> totals,
        List<ClassifiedDailyStat> perDay) {

    public SellerClassifiedStats {
        totals = List.copyOf(totals);
        perDay = List.copyOf(perDay);
    }

    /** Map the generated Layer-1 seller-statistics response to the public record. */
    public static SellerClassifiedStats from(SellerOfferStatsResponseDtoRaw raw) {
        return new SellerClassifiedStats(
                ClassifiedStatMappers.events(raw.getEventStatsTotal()),
                ClassifiedStatMappers.daily(raw.getEventsPerDay()));
    }
}
