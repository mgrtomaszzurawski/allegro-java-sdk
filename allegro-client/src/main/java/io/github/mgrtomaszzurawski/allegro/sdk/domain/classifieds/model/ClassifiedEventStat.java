/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ClassifiedEventStatRaw;

/**
 * The number of times one {@link ClassifiedEventType} occurred, either over the
 * whole period (a total) or within a single day.
 *
 * @param eventType the kind of event counted
 * @param count how many times it occurred
 *
 * @since 0.2.0
 */
public record ClassifiedEventStat(ClassifiedEventType eventType, int count) {

    private static final int NO_COUNT = 0;

    /** Map the generated Layer-1 DTO to the public record. */
    static ClassifiedEventStat from(ClassifiedEventStatRaw raw) {
        int count = raw.getCount() == null ? NO_COUNT : raw.getCount();
        return new ClassifiedEventStat(ClassifiedEventType.valueOf(raw.getEventType().name()), count);
    }
}
