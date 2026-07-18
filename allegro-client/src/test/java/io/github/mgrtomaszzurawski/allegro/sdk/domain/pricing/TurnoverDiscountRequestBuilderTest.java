/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.TurnoverDiscountRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.TurnoverThreshold;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Round-trip and fail-fast validation for {@link TurnoverDiscountRequest#builder()}:
 * at least one threshold is required, {@code addThreshold}/{@code thresholds}
 * both populate the ladder, and {@code toBuilder()} preserves it.
 */
class TurnoverDiscountRequestBuilderTest {

    private static final String TURNOVER_AMOUNT = "1000.00";
    private static final String TEST_CURRENCY = "PLN";
    private static final String DISCOUNT_PERCENTAGE = "5";
    private static final String THRESHOLD_TOKEN = "threshold";

    private static TurnoverThreshold threshold() {
        return new TurnoverThreshold(Money.of(TURNOVER_AMOUNT, TEST_CURRENCY), DISCOUNT_PERCENTAGE);
    }

    @Test
    void build_whenThresholdAdded_buildsRequest() {
        // when
        TurnoverDiscountRequest request = TurnoverDiscountRequest.builder()
                .addThreshold(threshold())
                .build();

        // then
        assertEquals(1, request.thresholds().size());
        assertEquals(threshold(), request.thresholds().get(0));
    }

    @Test
    void build_whenThresholdsListSet_replacesLadder() {
        // when
        TurnoverDiscountRequest request = TurnoverDiscountRequest.builder()
                .addThreshold(threshold())
                .thresholds(List.of(threshold(), threshold()))
                .build();

        // then — thresholds() replaced the single added threshold with the list
        assertEquals(2, request.thresholds().size());
    }

    @Test
    void toBuilder_preservesThresholds() {
        // given
        TurnoverDiscountRequest original = TurnoverDiscountRequest.builder()
                .addThreshold(threshold())
                .build();

        // when
        TurnoverDiscountRequest rebuilt = original.toBuilder().build();

        // then
        assertEquals(original, rebuilt);
    }

    @Test
    void build_whenNoThreshold_throwsIllegalState() {
        // given
        var builder = TurnoverDiscountRequest.builder();

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(THRESHOLD_TOKEN));
    }
}
