/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.PublicationSettings;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferStatus;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PublicationSettingsTest {

    private static final OffsetDateTime STARTING_AT =
            OffsetDateTime.of(2026, 8, 1, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final Duration DURATION = Duration.ofHours(72);

    @Test
    void build_whenAllFieldsSet_exposesEachValue() {
        // when
        PublicationSettings settings = PublicationSettings.builder()
                .status(OfferStatus.ACTIVE)
                .startingAt(STARTING_AT)
                .republish(Boolean.TRUE)
                .duration(DURATION)
                .build();

        // then
        assertEquals(OfferStatus.ACTIVE, settings.status());
        assertEquals(STARTING_AT, settings.startingAt());
        assertEquals(Boolean.TRUE, settings.republish());
        assertEquals(DURATION, settings.duration());
    }

    @Test
    void build_whenNothingSet_leavesEveryFieldNull() {
        // when
        PublicationSettings settings = PublicationSettings.builder().build();

        // then
        assertNull(settings.status());
        assertNull(settings.startingAt());
        assertNull(settings.republish());
        assertNull(settings.duration());
    }

    @Test
    void status_whenNonRequestableStatus_throwsAtSetTime() {
        // ACTIVATING/UNKNOWN are not client-requestable — the builder rejects them fail-fast,
        // not later at mapping time
        assertThrows(IllegalArgumentException.class,
                () -> PublicationSettings.builder().status(OfferStatus.ACTIVATING));
        assertThrows(IllegalArgumentException.class,
                () -> PublicationSettings.builder().status(OfferStatus.UNKNOWN));
    }

    @Test
    void toBuilder_whenRebuilt_preservesEveryField() {
        // given
        PublicationSettings original = PublicationSettings.builder()
                .status(OfferStatus.INACTIVE)
                .startingAt(STARTING_AT)
                .republish(Boolean.FALSE)
                .duration(DURATION)
                .build();

        // when
        PublicationSettings copy = original.toBuilder().build();

        // then
        assertEquals(original, copy);
    }
}
