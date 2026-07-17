/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder.WarrantyRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.WarrantyPeriod;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.WarrantyType;
import org.junit.jupiter.api.Test;

/**
 * Round-trip and fail-fast validation coverage for {@link WarrantyRequest} and
 * its builder — every builder method and every required-field / constraint
 * guard is exercised.
 */
class WarrantyRequestBuilderTest {

    private static final String NAME = "2 year seller warranty";
    private static final String DESCRIPTION = "Covers manufacturing defects";
    private static final String INDIVIDUAL_PERIOD = "P24M";
    private static final String ATTACHMENT_ID = "54702c96-4ccd-4c0e-b4c7-382a71e810b5";
    private static final String ATTACHMENT_NAME = "warranty.pdf";
    private static final int OVER_NAME_LIMIT = 201;
    private static final int OVER_DESCRIPTION_LIMIT = 10_241;
    private static final String FIELD_NAME = "name";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_DESCRIPTION = "description";

    @Test
    void build_whenRequiredFieldsOnly_succeeds() {
        // when
        WarrantyRequest request = WarrantyRequest.builder()
                .name(NAME)
                .type(WarrantyType.MANUFACTURER)
                .build();

        // then
        assertEquals(NAME, request.name());
        assertEquals(WarrantyType.MANUFACTURER, request.type());
        assertNull(request.individual());
        assertNull(request.corporate());
        assertNull(request.attachmentId());
        assertNull(request.description());
    }

    @Test
    void build_whenAllCoreFieldsSet_preservesEveryField() {
        // when
        WarrantyRequest request = WarrantyRequest.builder()
                .name(NAME)
                .type(WarrantyType.SELLER)
                .individual(WarrantyPeriod.of(INDIVIDUAL_PERIOD))
                .corporate(WarrantyPeriod.lifetimeWarranty())
                .attachment(ATTACHMENT_ID, ATTACHMENT_NAME)
                .description(DESCRIPTION)
                .build();

        // then
        assertEquals(WarrantyType.SELLER, request.type());
        assertEquals(INDIVIDUAL_PERIOD, request.individual().period());
        assertTrue(request.corporate().lifetime());
        assertEquals(ATTACHMENT_ID, request.attachmentId());
        assertEquals(ATTACHMENT_NAME, request.attachmentName());
        assertEquals(DESCRIPTION, request.description());
    }

    @Test
    void toBuilder_whenRebuilt_preservesFields() {
        // given
        WarrantyRequest original = WarrantyRequest.builder()
                .name(NAME)
                .type(WarrantyType.SELLER)
                .individual(WarrantyPeriod.of(INDIVIDUAL_PERIOD))
                .attachment(ATTACHMENT_ID, ATTACHMENT_NAME)
                .description(DESCRIPTION)
                .build();

        // when
        WarrantyRequest copy = original.toBuilder().build();

        // then
        assertEquals(original.name(), copy.name());
        assertEquals(original.type(), copy.type());
        assertEquals(original.individual().period(), copy.individual().period());
        assertEquals(original.attachmentId(), copy.attachmentId());
        assertEquals(original.description(), copy.description());
    }

    @Test
    void build_whenNameMissing_throws() {
        var builder = WarrantyRequest.builder().type(WarrantyType.SELLER);
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(FIELD_NAME));
    }

    @Test
    void build_whenNameBlank_throws() {
        var builder = WarrantyRequest.builder().name("  ").type(WarrantyType.SELLER);
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(FIELD_NAME));
    }

    @Test
    void build_whenTypeMissing_throws() {
        var builder = WarrantyRequest.builder().name(NAME);
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(FIELD_TYPE));
    }

    @Test
    void build_whenNameTooLong_throws() {
        var builder = WarrantyRequest.builder()
                .name("a".repeat(OVER_NAME_LIMIT))
                .type(WarrantyType.SELLER);
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(FIELD_NAME));
    }

    @Test
    void build_whenDescriptionTooLong_throws() {
        var builder = WarrantyRequest.builder()
                .name(NAME)
                .type(WarrantyType.SELLER)
                .description("a".repeat(OVER_DESCRIPTION_LIMIT));
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(FIELD_DESCRIPTION));
    }

    @Test
    void warrantyPeriod_factories_produceExpectedValues() {
        // when
        WarrantyPeriod fixed = WarrantyPeriod.of(INDIVIDUAL_PERIOD);
        WarrantyPeriod lifetime = WarrantyPeriod.lifetimeWarranty();

        // then
        assertEquals(INDIVIDUAL_PERIOD, fixed.period());
        assertFalse(fixed.lifetime());
        assertNull(lifetime.period());
        assertTrue(lifetime.lifetime());
    }
}
