/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.OpenHourRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder.OpenHourBuilder;

/**
 * Opening hours of a point of service on one day of the week. All three fields
 * are required by the Allegro contract. The {@code dayOfWeek} and the
 * {@code fromTime}/{@code toTime} values are kept as the raw wire strings (e.g.
 * {@code "MONDAY"}, {@code "08:00"}) rather than parsed types, so an unmodelled
 * server value never breaks a round-trip.
 *
 * @param dayOfWeek day of the week (e.g. {@code "MONDAY"})
 * @param fromTime opening time (e.g. {@code "08:00"})
 * @param toTime closing time (e.g. {@code "16:00"})
 *
 * @since 0.2.0
 */
public record OpenHour(String dayOfWeek, String fromTime, String toTime) {

    /** A fresh builder for an {@link OpenHour}. */
    public static OpenHourBuilder builder() {
        return new OpenHourBuilder();
    }

    /** A builder pre-loaded with this entry's fields. */
    public OpenHourBuilder toBuilder() {
        return new OpenHourBuilder().dayOfWeek(dayOfWeek).fromTime(fromTime).toTime(toTime);
    }

    /** Map the generated Layer-1 DTO to the public record. */
    public static OpenHour from(OpenHourRaw raw) {
        return new OpenHour(raw.getDayOfWeek(), raw.getFrom(), raw.getTo());
    }

    /** Build the generated Layer-1 DTO for a request body. */
    public OpenHourRaw toRaw() {
        OpenHourRaw raw = new OpenHourRaw();
        raw.setDayOfWeek(dayOfWeek);
        raw.setFrom(fromTime);
        raw.setTo(toTime);
        return raw;
    }
}
