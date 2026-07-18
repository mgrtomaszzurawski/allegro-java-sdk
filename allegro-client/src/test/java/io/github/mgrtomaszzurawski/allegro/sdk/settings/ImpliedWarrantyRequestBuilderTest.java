/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder.ImpliedWarrantyRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.AfterSalesAddress;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ImpliedWarrantyPeriod;
import org.junit.jupiter.api.Test;

/**
 * Round-trip and fail-fast validation coverage for {@link ImpliedWarrantyRequest}
 * and its builder — every builder method and every required-field / length guard
 * is exercised.
 */
class ImpliedWarrantyRequestBuilderTest {

    private static final String NAME = "2 year implied warranty";
    private static final String DESCRIPTION = "Statutory warranty of conformity";
    private static final String INDIVIDUAL_PERIOD = "P2Y";
    private static final String CORPORATE_PERIOD = "P1Y";
    private static final int NAME_AT_LIMIT = 200;
    private static final int OVER_NAME_LIMIT = 201;
    private static final int OVER_DESCRIPTION_LIMIT = 10_241;
    private static final String FIELD_NAME = "name";
    private static final String FIELD_INDIVIDUAL = "individual";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String LENGTH_WORD = "exceeds";

    private static AfterSalesAddress address() {
        return new AfterSalesAddress("Allegro sp. z o.o.", "Grunwaldzka 182", "60-166", "Poznań", "PL");
    }

    @Test
    void build_whenRequiredFieldsOnly_succeeds() {
        // when
        ImpliedWarrantyRequest request = ImpliedWarrantyRequest.builder()
                .name(NAME)
                .individual(ImpliedWarrantyPeriod.of(INDIVIDUAL_PERIOD))
                .build();

        // then
        assertEquals(NAME, request.name());
        assertEquals(INDIVIDUAL_PERIOD, request.individual().period());
        assertNull(request.corporate());
        assertNull(request.address());
        assertNull(request.description());
    }

    @Test
    void build_whenAllCoreFieldsSet_preservesEveryField() {
        // when
        ImpliedWarrantyRequest request = ImpliedWarrantyRequest.builder()
                .name(NAME)
                .individual(ImpliedWarrantyPeriod.of(INDIVIDUAL_PERIOD))
                .corporate(ImpliedWarrantyPeriod.of(CORPORATE_PERIOD))
                .address(address())
                .description(DESCRIPTION)
                .build();

        // then
        assertEquals(NAME, request.name());
        assertEquals(INDIVIDUAL_PERIOD, request.individual().period());
        assertEquals(CORPORATE_PERIOD, request.corporate().period());
        assertEquals("Poznań", request.address().city());
        assertEquals(DESCRIPTION, request.description());
    }

    @Test
    void toBuilder_whenRebuilt_preservesFields() {
        // given
        ImpliedWarrantyRequest original = ImpliedWarrantyRequest.builder()
                .name(NAME)
                .individual(ImpliedWarrantyPeriod.of(INDIVIDUAL_PERIOD))
                .corporate(ImpliedWarrantyPeriod.of(CORPORATE_PERIOD))
                .address(address())
                .description(DESCRIPTION)
                .build();

        // when
        ImpliedWarrantyRequest copy = original.toBuilder().build();

        // then
        assertEquals(original.name(), copy.name());
        assertEquals(original.individual().period(), copy.individual().period());
        assertEquals(original.corporate().period(), copy.corporate().period());
        assertEquals(original.address().street(), copy.address().street());
        assertEquals(original.description(), copy.description());
    }

    @Test
    void build_whenNameMissing_throws() {
        var builder = ImpliedWarrantyRequest.builder()
                .individual(ImpliedWarrantyPeriod.of(INDIVIDUAL_PERIOD));
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(FIELD_NAME));
    }

    @Test
    void build_whenNameBlank_throws() {
        var builder = ImpliedWarrantyRequest.builder()
                .name("  ")
                .individual(ImpliedWarrantyPeriod.of(INDIVIDUAL_PERIOD));
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(FIELD_NAME));
    }

    @Test
    void build_whenIndividualMissing_throws() {
        var builder = ImpliedWarrantyRequest.builder().name(NAME);
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(FIELD_INDIVIDUAL));
    }

    @Test
    void build_whenNameAtLimit_succeeds() {
        // Boundary: a name of exactly the cap length must be accepted.
        String maxName = "a".repeat(NAME_AT_LIMIT);
        ImpliedWarrantyRequest request = ImpliedWarrantyRequest.builder()
                .name(maxName)
                .individual(ImpliedWarrantyPeriod.of(INDIVIDUAL_PERIOD))
                .build();
        assertEquals(maxName, request.name());
    }

    @Test
    void build_whenNameTooLong_throws() {
        var builder = ImpliedWarrantyRequest.builder()
                .name("a".repeat(OVER_NAME_LIMIT))
                .individual(ImpliedWarrantyPeriod.of(INDIVIDUAL_PERIOD));
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        // Pin the length branch, not the required-field branch (both mention "name").
        assertTrue(failure.getMessage().contains(FIELD_NAME));
        assertTrue(failure.getMessage().contains(LENGTH_WORD));
    }

    @Test
    void build_whenDescriptionTooLong_throws() {
        var builder = ImpliedWarrantyRequest.builder()
                .name(NAME)
                .individual(ImpliedWarrantyPeriod.of(INDIVIDUAL_PERIOD))
                .description("a".repeat(OVER_DESCRIPTION_LIMIT));
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(FIELD_DESCRIPTION));
    }
}
