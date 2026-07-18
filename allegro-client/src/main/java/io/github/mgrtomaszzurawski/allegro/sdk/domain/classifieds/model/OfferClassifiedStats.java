/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model;

import io.github.mgrtomaszzurawski.allegro.client.model.OfferStatResponseDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferStatsResponseDtoRaw;
import java.util.List;

/**
 * The advertisement statistics for a single offer, as returned by
 * {@code Classifieds.offerStats(...)}: the per-event totals over the requested
 * period plus the day-by-day breakdown.
 *
 * @param offerId identifier of the offer these statistics belong to
 * @param totals the per-event-type totals over the period; never {@code null},
 *     possibly empty
 * @param perDay the day-by-day breakdown; never {@code null}, possibly empty
 *
 * @since 0.2.0
 */
public record OfferClassifiedStats(
        String offerId,
        List<ClassifiedEventStat> totals,
        List<ClassifiedDailyStat> perDay) {

    public OfferClassifiedStats {
        totals = List.copyOf(totals);
        perDay = List.copyOf(perDay);
    }

    /** Map one generated Layer-1 offer-statistics DTO to the public record. */
    public static OfferClassifiedStats from(OfferStatResponseDtoRaw raw) {
        return new OfferClassifiedStats(
                raw.getOffer().getId(),
                ClassifiedStatMappers.events(raw.getEventStatsTotal()),
                ClassifiedStatMappers.daily(raw.getEventsPerDay()));
    }

    /**
     * Map the generated Layer-1 list response to public records, skipping any
     * entry without an offer (the field is spec-optional, so a stats block that
     * cannot be attributed to an offer id is dropped rather than surfaced).
     */
    public static List<OfferClassifiedStats> listFrom(OfferStatsResponseDtoRaw raw) {
        return raw.getOfferStats() == null
                ? List.of()
                : raw.getOfferStats().stream()
                        .filter(offerStat -> offerStat.getOffer() != null)
                        .map(OfferClassifiedStats::from)
                        .toList();
    }
}
