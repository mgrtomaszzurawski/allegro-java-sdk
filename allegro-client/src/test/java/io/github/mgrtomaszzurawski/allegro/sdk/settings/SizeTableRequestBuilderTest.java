/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.builder.SizeTableRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.builder.SizeTableRequestBuilder;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.model.SizeTableRow;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Round-trip and required-field coverage of {@link SizeTableRequest}'s builder. */
class SizeTableRequestBuilderTest {

    private static final String NAME = "Nike shoes size table";
    private static final String TEMPLATE_ID = "5637592a-0a24-4771-b527-d89b2767d821";
    private static final String HEADER_SIZE = "Size";
    private static final String HEADER_LENGTH = "Length";
    private static final String CELL_SIZE = "M";
    private static final String CELL_LENGTH = "38";

    private static SizeTableRequestBuilder baseBuilder() {
        return SizeTableRequest.builder()
                .name(NAME)
                .templateId(TEMPLATE_ID)
                .header(HEADER_SIZE)
                .header(HEADER_LENGTH)
                .row(List.of(CELL_SIZE, CELL_LENGTH));
    }

    @Test
    void build_whenRequiredFieldsOnly_succeeds() {
        SizeTableRequest request = SizeTableRequest.builder()
                .name(NAME)
                .header(HEADER_SIZE)
                .row(List.of(CELL_SIZE))
                .build();

        assertEquals(NAME, request.name());
        assertEquals(1, request.headers().size());
        assertEquals(1, request.rows().size());
    }

    @Test
    void build_whenAllCoreFieldsSet_preservesValues() {
        SizeTableRequest request = baseBuilder().build();

        assertEquals(NAME, request.name());
        assertEquals(TEMPLATE_ID, request.templateId());
        assertEquals(List.of(HEADER_SIZE, HEADER_LENGTH), request.headers());
        assertEquals(1, request.rows().size());
        assertEquals(List.of(CELL_SIZE, CELL_LENGTH), request.rows().get(0).cells());
    }

    @Test
    void toBuilder_whenRebuilt_preservesEveryField() {
        SizeTableRequest original = baseBuilder().build();

        SizeTableRequest copy = original.toBuilder().build();

        assertEquals(original, copy);
    }

    @Test
    void build_whenNameMissing_throws() {
        SizeTableRequestBuilder builder = SizeTableRequest.builder().header(HEADER_SIZE).row(List.of(CELL_SIZE));

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void build_whenNoHeader_throws() {
        SizeTableRequestBuilder builder = SizeTableRequest.builder().name(NAME).row(List.of(CELL_SIZE));

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void build_whenNoRow_throws() {
        SizeTableRequestBuilder builder = SizeTableRequest.builder().name(NAME).header(HEADER_SIZE);

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void row_whenAddedAsModel_matchesCellList() {
        SizeTableRequest request = SizeTableRequest.builder()
                .name(NAME)
                .header(HEADER_SIZE)
                .row(SizeTableRow.of(List.of(CELL_SIZE)))
                .build();

        assertTrue(request.rows().get(0).cells().contains(CELL_SIZE));
    }
}
