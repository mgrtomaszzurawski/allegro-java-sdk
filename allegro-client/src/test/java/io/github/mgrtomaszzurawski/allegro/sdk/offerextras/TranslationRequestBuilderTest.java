/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offerextras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.TranslationRequest;
import org.junit.jupiter.api.Test;

/**
 * Round-trip coverage of {@link TranslationRequest.builder()}: the required
 * title, the {@code toBuilder} copy, and the fail-fast on a missing/blank title.
 */
class TranslationRequestBuilderTest {

    private static final String TITLE = "Wireless keyboard";
    private static final String MISSING_TITLE_MESSAGE = "title is required";
    private static final String BLANK = "   ";

    @Test
    void build_whenTitleGiven_keepsTitle() {
        // when
        TranslationRequest request = TranslationRequest.builder().title(TITLE).build();

        // then
        assertEquals(TITLE, request.title());
    }

    @Test
    void toBuilder_whenRebuilt_preservesTitle() {
        // given
        TranslationRequest original = TranslationRequest.builder().title(TITLE).build();

        // when
        TranslationRequest copy = original.toBuilder().build();

        // then
        assertEquals(original, copy);
    }

    @Test
    void build_whenTitleMissing_throwsIllegalState() {
        var builder = TranslationRequest.builder();

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertEquals(MISSING_TITLE_MESSAGE, failure.getMessage());
    }

    @Test
    void build_whenTitleBlank_throwsIllegalState() {
        var builder = TranslationRequest.builder().title(BLANK);

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertEquals(MISSING_TITLE_MESSAGE, failure.getMessage());
    }
}
