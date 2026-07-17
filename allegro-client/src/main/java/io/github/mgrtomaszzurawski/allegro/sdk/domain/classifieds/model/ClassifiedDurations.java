/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.model;

import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import java.time.Duration;
import java.time.format.DateTimeParseException;

/**
 * Parses the ISO-8601 duration strings Allegro returns for classifieds
 * packages. A malformed value is surfaced as a typed {@link AllegroServerException}
 * (a response-contract violation) rather than a raw JDK
 * {@link DateTimeParseException} escaping the SDK surface.
 */
final class ClassifiedDurations {

    private static final String ERR_MALFORMED_DURATION =
            "Malformed ISO-8601 duration in the Allegro classifieds response";

    private ClassifiedDurations() {
    }

    static Duration parse(String iso8601Duration) {
        try {
            return Duration.parse(iso8601Duration);
        } catch (DateTimeParseException e) {
            throw new AllegroServerException(ERR_MALFORMED_DURATION, e);
        }
    }
}
