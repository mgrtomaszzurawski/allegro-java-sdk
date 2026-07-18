/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.classifieds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.classifieds.builder.ClassifiedStatsFilter;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

/**
 * Round-trip coverage of {@link ClassifiedStatsFilter}: the all-optional bounds,
 * the {@code all()} shortcut, and the {@code toBuilder} copy.
 */
class ClassifiedStatsFilterTest {

    private static final OffsetDateTime FROM = OffsetDateTime.parse("2026-07-01T10:15:30Z");
    private static final OffsetDateTime TO = OffsetDateTime.parse("2026-07-08T10:15:30Z");

    @Test
    void all_whenBuilt_hasNoBounds() {
        // when
        ClassifiedStatsFilter filter = ClassifiedStatsFilter.all();

        // then
        assertNull(filter.eventsFrom());
        assertNull(filter.eventsTo());
    }

    @Test
    void build_whenBoundsSet_keepsBothBounds() {
        // when
        ClassifiedStatsFilter filter = ClassifiedStatsFilter.builder().eventsFrom(FROM).eventsTo(TO).build();

        // then
        assertEquals(FROM, filter.eventsFrom());
        assertEquals(TO, filter.eventsTo());
    }

    @Test
    void toBuilder_whenRebuilt_preservesBounds() {
        // given
        ClassifiedStatsFilter original = ClassifiedStatsFilter.builder().eventsFrom(FROM).eventsTo(TO).build();

        // when
        ClassifiedStatsFilter copy = original.toBuilder().build();

        // then
        assertEquals(FROM, copy.eventsFrom());
        assertEquals(TO, copy.eventsTo());
    }
}
