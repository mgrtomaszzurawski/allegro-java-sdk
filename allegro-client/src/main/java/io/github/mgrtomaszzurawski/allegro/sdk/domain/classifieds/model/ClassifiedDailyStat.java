/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ClassifiedDailyEventStatResponseDtoRaw;
import java.time.LocalDate;
import java.util.List;

/**
 * The classifieds statistics collected on a single day: the per-event counts
 * for that day.
 *
 * @param date the day the events were counted on
 * @param events the per-event-type counts for the day; never {@code null},
 *     possibly empty
 *
 * @since 0.2.0
 */
public record ClassifiedDailyStat(LocalDate date, List<ClassifiedEventStat> events) {

    public ClassifiedDailyStat {
        events = List.copyOf(events);
    }

    /** Map the generated Layer-1 DTO to the public record. */
    static ClassifiedDailyStat from(ClassifiedDailyEventStatResponseDtoRaw raw) {
        return new ClassifiedDailyStat(
                ClassifiedDates.parse(raw.getDate()),
                ClassifiedStatMappers.events(raw.getEventStats()));
    }
}
