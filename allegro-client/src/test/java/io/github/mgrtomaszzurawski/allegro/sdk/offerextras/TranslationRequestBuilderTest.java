/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offerextras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.TranslationRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.DescriptionSection;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.DescriptionSectionItem;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.ProductSafetyInformationTranslation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.StandardizedDescription;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Round-trip coverage of {@link TranslationRequest#builder()}: each part builds on
 * its own, {@code toBuilder} copies every part, and the fail-fast rejects an empty
 * update, a blank title, and an empty safety-information list.
 */
class TranslationRequestBuilderTest {

    private static final String TITLE = "Wireless keyboard";
    private static final String BLANK = "   ";
    private static final String PRODUCT_ID = "8a2b4c6d-0e1f-2a3b-4c5d-6e7f8a9b0c1d";
    private static final String SAFETY_TEXT = "Handle with care";
    private static final String DESCRIPTION_TEXT = "A great keyboard";

    private static final String EMPTY_MESSAGE =
            "a translation update must set at least one of title, description, or safety information";
    private static final String BLANK_TITLE_MESSAGE = "title must not be blank when set";
    private static final String SAFETY_EMPTY_MESSAGE = "safetyInformation must not be empty when set";

    private static StandardizedDescription description() {
        return StandardizedDescription.of(DescriptionSection.of(DescriptionSectionItem.text(DESCRIPTION_TEXT)));
    }

    private static List<ProductSafetyInformationTranslation> safety() {
        return List.of(ProductSafetyInformationTranslation.of(PRODUCT_ID, SAFETY_TEXT));
    }

    @Test
    void build_whenTitleOnly_keepsTitle() {
        // when
        TranslationRequest request = TranslationRequest.builder().title(TITLE).build();

        // then
        assertEquals(TITLE, request.title());
    }

    @Test
    void build_whenDescriptionOnly_keepsDescription() {
        // when
        TranslationRequest request = TranslationRequest.builder().description(description()).build();

        // then
        assertEquals(description(), request.description());
    }

    @Test
    void build_whenSafetyOnly_keepsSafety() {
        // when
        TranslationRequest request = TranslationRequest.builder().safetyInformation(safety()).build();

        // then
        assertEquals(safety(), request.safetyInformation());
    }

    @Test
    void build_whenAllPartsGiven_keepsAll() {
        // when
        TranslationRequest request = TranslationRequest.builder()
                .title(TITLE)
                .description(description())
                .safetyInformation(safety())
                .build();

        // then
        assertEquals(TITLE, request.title());
        assertEquals(description(), request.description());
        assertEquals(safety(), request.safetyInformation());
    }

    @Test
    void toBuilder_whenRebuilt_preservesAllParts() {
        // given
        TranslationRequest original = TranslationRequest.builder()
                .title(TITLE)
                .description(description())
                .safetyInformation(safety())
                .build();

        // when
        TranslationRequest copy = original.toBuilder().build();

        // then
        assertEquals(original, copy);
    }

    @Test
    void build_whenNoPartSet_throwsIllegalState() {
        var builder = TranslationRequest.builder();

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertEquals(EMPTY_MESSAGE, failure.getMessage());
    }

    @Test
    void build_whenTitleBlank_throwsIllegalState() {
        var builder = TranslationRequest.builder().title(BLANK);

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertEquals(BLANK_TITLE_MESSAGE, failure.getMessage());
    }

    @Test
    void build_whenSafetyEmpty_throwsIllegalState() {
        var builder = TranslationRequest.builder().safetyInformation(List.of());

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertEquals(SAFETY_EMPTY_MESSAGE, failure.getMessage());
    }
}
