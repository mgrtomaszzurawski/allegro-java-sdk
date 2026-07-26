/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.PickupTimeDtoRaw;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A pickup time window on a given day: the date plus the optional earliest and
 * latest times the carrier can collect. It appears both in a pickup request
 * (the window the seller asks for) and in a pickup proposal (a window the
 * carrier offers).
 *
 * @param date the pickup date (ISO {@code yyyy-MM-dd})
 * @param minTime the earliest collection time (e.g. {@code "08:00"}), or {@code null}
 * @param maxTime the latest collection time (e.g. {@code "16:00"}), or {@code null}
 *
 * @since 0.5.0
 */
public record PickupTime(
        String date,
        @Nullable String minTime,
        @Nullable String maxTime) {

    private static final String DATE_REQUIRED = "PickupTime.date is required";

    /** Canonical constructor: the date is required. */
    public PickupTime {
        Objects.requireNonNull(date, DATE_REQUIRED);
    }

    /** A window on {@code date} between {@code minTime} and {@code maxTime}. */
    public static PickupTime of(String date, @Nullable String minTime, @Nullable String maxTime) {
        return new PickupTime(date, minTime, maxTime);
    }

    /** Map the generated DTO to the public record. */
    public static PickupTime from(PickupTimeDtoRaw raw) {
        return new PickupTime(raw.getDate(), raw.getMinTime(), raw.getMaxTime());
    }

    /** Build the generated DTO for a request body. */
    public PickupTimeDtoRaw toRaw() {
        PickupTimeDtoRaw raw = new PickupTimeDtoRaw();
        raw.setDate(date);
        raw.setMinTime(minTime);
        raw.setMaxTime(maxTime);
        return raw;
    }
}
