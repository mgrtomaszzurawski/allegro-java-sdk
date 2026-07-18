/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model;

import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Parses the {@code yyyy-MM-dd} day strings Allegro returns for classifieds
 * daily statistics. A malformed value is surfaced as a typed
 * {@link AllegroServerException} (a response-contract violation) rather than a
 * raw JDK {@link DateTimeParseException} escaping the SDK surface.
 */
final class ClassifiedDates {

    private static final String ERR_MALFORMED_DATE =
            "Malformed yyyy-MM-dd date in the Allegro classifieds statistics response";

    private ClassifiedDates() {
    }

    static LocalDate parse(String isoDate) {
        try {
            return LocalDate.parse(isoDate);
        } catch (DateTimeParseException e) {
            throw new AllegroServerException(ERR_MALFORMED_DATE, e);
        }
    }
}
