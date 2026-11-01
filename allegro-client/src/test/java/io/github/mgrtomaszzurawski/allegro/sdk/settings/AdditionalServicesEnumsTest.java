/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mgrtomaszzurawski.allegro.client.model.ConstraintCriteriaRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model.ServiceConstraintType;
import org.junit.jupiter.api.Test;

/**
 * Parity between the strict {@link ServiceConstraintType} mapper and the generated
 * Layer-1 enum: every constraint type the spec declares must map both ways, so the
 * next spec regeneration that adds a value fails here instead of on a live group
 * read ({@code from()} is a strict {@code valueOf}).
 */
class AdditionalServicesEnumsTest {

    @Test
    void from_whenEveryGeneratedValue_mapsToDomainConstantOfTheSameName() {
        for (ConstraintCriteriaRaw.TypeEnum raw : ConstraintCriteriaRaw.TypeEnum.values()) {
            if (raw == ConstraintCriteriaRaw.TypeEnum.UNKNOWN_DEFAULT_OPEN_API) {
                continue;
            }
            // given a value the regenerated spec declares; when mapped; then the names coincide
            assertEquals(raw.name(), ServiceConstraintType.from(raw).name());
        }
    }

    @Test
    void toRaw_whenEveryDomainConstant_roundTripsThroughFrom() {
        for (ServiceConstraintType type : ServiceConstraintType.values()) {
            // given a domain constant; when projected to the wire and read back; then it round-trips
            assertEquals(type, ServiceConstraintType.from(type.toRaw()));
        }
    }

    @Test
    void from_whenGeneratedUnknownSentinel_failsLoudly() {
        // given the generator's sentinel for a wire value no constant models
        ConstraintCriteriaRaw.TypeEnum sentinel = ConstraintCriteriaRaw.TypeEnum.UNKNOWN_DEFAULT_OPEN_API;

        // when / then — the mapper is deliberately strict (documented on from())
        assertThrows(IllegalArgumentException.class, () -> ServiceConstraintType.from(sentinel));
    }
}
