/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offerextras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.TagRequest;
import org.junit.jupiter.api.Test;

/**
 * Round-trip coverage of {@link TagRequest.builder()}: the required name, the
 * optional hidden flag, the {@code toBuilder} copy, and the fail-fast on the
 * missing required name.
 */
class TagRequestBuilderTest {

    private static final String TAG_NAME = "Priority";
    private static final String MISSING_NAME_MESSAGE = "name is required";
    private static final String BLANK = "   ";

    @Test
    void build_whenOnlyName_leavesHiddenUnset() {
        // when
        TagRequest request = TagRequest.builder().name(TAG_NAME).build();

        // then
        assertEquals(TAG_NAME, request.name());
        assertNull(request.hidden());
    }

    @Test
    void build_whenNameAndHidden_keepsBoth() {
        // when
        TagRequest request = TagRequest.builder().name(TAG_NAME).hidden(true).build();

        // then
        assertEquals(TAG_NAME, request.name());
        assertEquals(Boolean.TRUE, request.hidden());
    }

    @Test
    void toBuilder_whenRebuilt_preservesFields() {
        // given
        TagRequest original = TagRequest.builder().name(TAG_NAME).hidden(false).build();

        // when
        TagRequest copy = original.toBuilder().build();

        // then
        assertEquals(original, copy);
    }

    @Test
    void build_whenNameMissing_throwsIllegalState() {
        var builder = TagRequest.builder().hidden(true);

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertEquals(MISSING_NAME_MESSAGE, failure.getMessage());
    }

    @Test
    void build_whenNameBlank_throwsIllegalState() {
        var builder = TagRequest.builder().name(BLANK);

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertEquals(MISSING_NAME_MESSAGE, failure.getMessage());
    }
}
