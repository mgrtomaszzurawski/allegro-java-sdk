/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.OpenHour;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for one {@link OpenHour} entry. All three fields
 * ({@code dayOfWeek}, {@code fromTime}, {@code toTime}) are required.
 *
 * @since 0.2.0
 */
public final class OpenHourBuilder {

    private static final String FIELD_DAY_OF_WEEK = "OpenHour.dayOfWeek";
    private static final String FIELD_FROM_TIME = "OpenHour.fromTime";
    private static final String FIELD_TO_TIME = "OpenHour.toTime";

    private @Nullable String dayOfWeek;
    private @Nullable String fromTime;
    private @Nullable String toTime;

    /** Day of the week, e.g. {@code "MONDAY"} (required). */
    public OpenHourBuilder dayOfWeek(@Nullable String value) {
        this.dayOfWeek = value;
        return this;
    }

    /** Opening time, e.g. {@code "08:00"} (required). */
    public OpenHourBuilder fromTime(@Nullable String value) {
        this.fromTime = value;
        return this;
    }

    /** Closing time, e.g. {@code "16:00"} (required). */
    public OpenHourBuilder toTime(@Nullable String value) {
        this.toTime = value;
        return this;
    }

    /**
     * Validate and assemble the immutable {@link OpenHour}.
     *
     * @throws IllegalStateException if any field is missing
     */
    public OpenHour build() {
        return new OpenHour(
                BuilderValidation.requireText(dayOfWeek, FIELD_DAY_OF_WEEK),
                BuilderValidation.requireText(fromTime, FIELD_FROM_TIME),
                BuilderValidation.requireText(toTime, FIELD_TO_TIME));
    }
}
