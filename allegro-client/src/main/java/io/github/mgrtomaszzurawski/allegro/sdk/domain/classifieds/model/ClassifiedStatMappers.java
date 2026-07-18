/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ClassifiedDailyEventStatResponseDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ClassifiedEventStatRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Shared null-safe list mappings for the classifieds statistics records: the
 * per-event totals and the per-day breakdown appear identically on both the
 * offer and the seller statistics responses.
 */
final class ClassifiedStatMappers {

    private ClassifiedStatMappers() {
    }

    /**
     * Map a list of raw event counts, treating an absent list as empty and
     * skipping any entry without an event type (the field is spec-optional, so a
     * type-less count cannot be attributed to a {@code ClassifiedEventType}).
     */
    static List<ClassifiedEventStat> events(@Nullable List<ClassifiedEventStatRaw> raw) {
        return raw == null
                ? List.of()
                : raw.stream()
                        .filter(stat -> stat.getEventType() != null)
                        .map(ClassifiedEventStat::from)
                        .toList();
    }

    /** Map a list of raw daily statistics, treating an absent list as empty. */
    static List<ClassifiedDailyStat> daily(@Nullable List<ClassifiedDailyEventStatResponseDtoRaw> raw) {
        return raw == null ? List.of() : raw.stream().map(ClassifiedDailyStat::from).toList();
    }
}
