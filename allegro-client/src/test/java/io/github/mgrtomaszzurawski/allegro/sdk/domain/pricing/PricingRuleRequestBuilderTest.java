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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleType;
import org.junit.jupiter.api.Test;

/**
 * Round-trip and fail-fast validation for {@link PricingRuleRequest#builder()}:
 * required fields, the replicated 33-character name limit, and
 * {@code toBuilder()} field preservation.
 */
class PricingRuleRequestBuilderTest {

    private static final String TEST_NAME = "Follow Allegro minus 5%";
    private static final int NAME_MAX_LENGTH = 33;
    private static final int NAME_OVER_MAX = 34;
    private static final String TEST_PERCENTAGE = "5";
    private static final String NAME_TOKEN = "name";
    private static final String TYPE_TOKEN = "type";

    private static PricingRuleConfiguration percentageConfiguration() {
        return new PricingRuleConfiguration.ChangeByPercentage(
                PricingRuleConfiguration.Operation.SUBTRACT, TEST_PERCENTAGE);
    }

    @Test
    void build_whenRequiredFieldsOnly_buildsRequestWithoutConfiguration() {
        // when
        PricingRuleRequest request = PricingRuleRequest.builder()
                .name(TEST_NAME)
                .type(PricingRuleType.FOLLOW_BY_ALLEGRO_MIN_PRICE)
                .build();

        // then
        assertEquals(TEST_NAME, request.name());
        assertEquals(PricingRuleType.FOLLOW_BY_ALLEGRO_MIN_PRICE, request.type());
        assertNull(request.configuration());
    }

    @Test
    void build_whenAllCoreFieldsSet_buildsRequestWithConfiguration() {
        // when
        PricingRuleRequest request = PricingRuleRequest.builder()
                .name(TEST_NAME)
                .type(PricingRuleType.FOLLOW_BY_MARKET_MIN_PRICE)
                .configuration(percentageConfiguration())
                .build();

        // then
        assertEquals(percentageConfiguration(), request.configuration());
    }

    @Test
    void toBuilder_preservesAllFields() {
        // given
        PricingRuleRequest original = PricingRuleRequest.builder()
                .name(TEST_NAME)
                .type(PricingRuleType.FOLLOW_BY_TOP_OFFER_PRICE)
                .configuration(percentageConfiguration())
                .build();

        // when
        PricingRuleRequest rebuilt = original.toBuilder().build();

        // then
        assertEquals(original, rebuilt);
    }

    @Test
    void build_whenNameMissing_throwsIllegalState() {
        // given
        var builder = PricingRuleRequest.builder().type(PricingRuleType.FOLLOW_BY_ALLEGRO_MIN_PRICE);

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(NAME_TOKEN));
    }

    @Test
    void build_whenNameBlank_throwsIllegalState() {
        // given
        var builder = PricingRuleRequest.builder()
                .name(" ")
                .type(PricingRuleType.FOLLOW_BY_ALLEGRO_MIN_PRICE);

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(NAME_TOKEN));
    }

    @Test
    void build_whenTypeMissing_throwsIllegalState() {
        // given
        var builder = PricingRuleRequest.builder().name(TEST_NAME);

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(TYPE_TOKEN));
    }

    @Test
    void build_whenNameAtMaxLength_succeeds() {
        // given — 33 characters is the server limit and must be accepted
        PricingRuleRequest request = PricingRuleRequest.builder()
                .name("a".repeat(NAME_MAX_LENGTH))
                .type(PricingRuleType.FOLLOW_BY_ALLEGRO_MIN_PRICE)
                .build();

        // then
        assertEquals(NAME_MAX_LENGTH, request.name().length());
    }

    @Test
    void build_whenNameTooLong_throwsIllegalState() {
        // given — one character over the limit
        var builder = PricingRuleRequest.builder()
                .name("a".repeat(NAME_OVER_MAX))
                .type(PricingRuleType.FOLLOW_BY_ALLEGRO_MIN_PRICE);

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(NAME_TOKEN));
    }
}
