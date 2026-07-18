/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleConfiguration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleEdit;
import org.junit.jupiter.api.Test;

/**
 * Round-trip and fail-fast validation for {@link PricingRuleEdit#builder()}:
 * the required name, the replicated 33-character limit, {@code toBuilder()}
 * preservation, and the deliberate absence of a rule type (immutable after
 * creation).
 */
class PricingRuleEditBuilderTest {

    private static final String TEST_NAME = "Follow Allegro minus 8%";
    private static final int NAME_MAX_LENGTH = 33;
    private static final int NAME_OVER_MAX = 34;
    private static final String TEST_PERCENTAGE = "8";
    private static final String NAME_TOKEN = "name";

    private static PricingRuleConfiguration percentageConfiguration() {
        return new PricingRuleConfiguration.ChangeByPercentage(
                PricingRuleConfiguration.Operation.SUBTRACT, TEST_PERCENTAGE);
    }

    @Test
    void build_whenNameOnly_buildsEditWithoutConfiguration() {
        // when
        PricingRuleEdit edit = PricingRuleEdit.builder()
                .name(TEST_NAME)
                .build();

        // then
        assertEquals(TEST_NAME, edit.name());
        assertNull(edit.configuration());
    }

    @Test
    void build_whenNameAndConfiguration_buildsEditWithConfiguration() {
        // when
        PricingRuleEdit edit = PricingRuleEdit.builder()
                .name(TEST_NAME)
                .configuration(percentageConfiguration())
                .build();

        // then
        assertEquals(percentageConfiguration(), edit.configuration());
    }

    @Test
    void toBuilder_preservesAllFields() {
        // given
        PricingRuleEdit original = PricingRuleEdit.builder()
                .name(TEST_NAME)
                .configuration(percentageConfiguration())
                .build();

        // when
        PricingRuleEdit rebuilt = original.toBuilder().build();

        // then
        assertEquals(original, rebuilt);
    }

    @Test
    void build_whenNameMissing_throwsIllegalState() {
        // given
        var builder = PricingRuleEdit.builder().configuration(percentageConfiguration());

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(NAME_TOKEN));
    }

    @Test
    void build_whenNameBlank_throwsIllegalState() {
        // given
        var builder = PricingRuleEdit.builder().name(" ");

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(NAME_TOKEN));
    }

    @Test
    void build_whenNameAtMaxLength_succeeds() {
        // given — 33 characters is the server limit and must be accepted
        String maxLengthName = "a".repeat(NAME_MAX_LENGTH);
        PricingRuleEdit edit = PricingRuleEdit.builder()
                .name(maxLengthName)
                .build();

        // then
        assertEquals(maxLengthName, edit.name());
    }

    @Test
    void build_whenNameTooLong_throwsIllegalState() {
        // given — one character over the limit
        var builder = PricingRuleEdit.builder().name("a".repeat(NAME_OVER_MAX));

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(NAME_TOKEN));
    }
}
