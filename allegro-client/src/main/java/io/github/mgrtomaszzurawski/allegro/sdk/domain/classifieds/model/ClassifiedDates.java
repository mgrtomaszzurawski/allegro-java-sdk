/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model;

import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.jspecify.annotations.Nullable;

/**
 * Parses the {@code yyyy-MM-dd} day strings Allegro returns for classifieds
 * daily statistics. A missing or malformed value is surfaced as a typed
 * {@link AllegroServerException} (a response-contract violation) rather than a
 * raw JDK {@link NullPointerException}/{@link DateTimeParseException} escaping
 * the SDK surface.
 */
final class ClassifiedDates {

    private static final String ERR_MALFORMED_DATE =
            "Missing or malformed yyyy-MM-dd date in the Allegro classifieds statistics response";

    private ClassifiedDates() {
    }

    static LocalDate parse(@Nullable String isoDate) {
        if (isoDate == null) {
            throw new AllegroServerException(ERR_MALFORMED_DATE, null);
        }
        try {
            return LocalDate.parse(isoDate);
        } catch (DateTimeParseException e) {
            throw new AllegroServerException(ERR_MALFORMED_DATE, e);
        }
    }
}
