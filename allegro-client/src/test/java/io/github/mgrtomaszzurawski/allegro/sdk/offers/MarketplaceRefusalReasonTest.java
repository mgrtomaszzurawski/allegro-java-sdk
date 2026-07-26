/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalMarketplacesRefusalReasonResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.MarketplaceRefusalReason;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Projection of a marketplace publication refusal reason. */
class MarketplaceRefusalReasonTest {

    private static final String CODE = "PRICE_TOO_LOW";
    private static final String MESSAGE = "The price is below the marketplace minimum.";
    private static final String PARAM_KEY = "min";
    private static final String PARAM_VALUE = "1000.00";

    @Test
    void from_whenPopulated_mapsCodeMessageAndParameters() {
        // given a refusal reason with parameters
        AdditionalMarketplacesRefusalReasonResponseRaw raw = new AdditionalMarketplacesRefusalReasonResponseRaw()
                .code(CODE).userMessage(MESSAGE).parameters(Map.of(PARAM_KEY, List.of(PARAM_VALUE)));

        // when
        MarketplaceRefusalReason reason = MarketplaceRefusalReason.from(raw);

        // then
        assertEquals(CODE, reason.code());
        assertEquals(MESSAGE, reason.userMessage());
        assertEquals(List.of(PARAM_VALUE), reason.parameters().get(PARAM_KEY));
    }

    @Test
    void from_whenParametersNull_yieldsEmptyMap() {
        // given a refusal reason with an explicit null parameters block (spec-legal: nullable)
        AdditionalMarketplacesRefusalReasonResponseRaw raw =
                new AdditionalMarketplacesRefusalReasonResponseRaw().code(CODE).parameters(null);

        // when projected; then the null degrades to an empty map (not null, not a throw)
        assertTrue(MarketplaceRefusalReason.from(raw).parameters().isEmpty());
    }
}
