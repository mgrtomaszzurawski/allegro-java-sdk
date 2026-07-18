/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.transport;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.mgrtomaszzurawski.allegro.client.model.AutomaticPricingRuleConfigurationChangeByAmountRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AutomaticPricingRuleConfigurationRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.StrictOneOfModule;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullableModule;

/**
 * Pins the strict {@code oneOf} resolution. {@code AutomaticPricingRuleConfigurationRaw}
 * is a {@code oneOf[changeByAmount, changeByPercentage]}; a {@code {"changeByAmount":{}}}
 * payload has a property foreign to the percentage branch. Under the SDK's lenient
 * mapper both branches falsely match ("2 classes match"); the module makes only the
 * amount branch match.
 */
class StrictOneOfModuleTest {

    private static final String CHANGE_BY_AMOUNT_JSON = "{\"changeByAmount\":{}}";

    /** The SDK mapper config WITHOUT the strict-oneOf module (forward-compat lenient). */
    private static ObjectMapper lenientBaseMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new JsonNullableModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    void deserialize_whenStructuralOneOfWithoutModule_overMatchesAndFails() {
        // given — the lenient mapper the generated deserializer trials branches with
        ObjectMapper lenient = lenientBaseMapper();

        // when / then — the percentage branch also "matches" by ignoring changeByAmount,
        // so the generated deserializer throws "2 classes match, expected 1"
        IOException failure = assertThrows(IOException.class, () ->
                lenient.readValue(CHANGE_BY_AMOUNT_JSON, AutomaticPricingRuleConfigurationRaw.class));
        assertInstanceOf(IOException.class, failure);
    }

    @Test
    void deserialize_whenStructuralOneOfWithModule_resolvesTheMatchingBranch() throws IOException {
        // given — the same mapper plus the strict-oneOf module
        ObjectMapper strict = lenientBaseMapper().registerModule(new StrictOneOfModule());

        // when
        AutomaticPricingRuleConfigurationRaw result =
                strict.readValue(CHANGE_BY_AMOUNT_JSON, AutomaticPricingRuleConfigurationRaw.class);

        // then — only the amount branch survives strict matching, no over-match
        assertInstanceOf(AutomaticPricingRuleConfigurationChangeByAmountRaw.class,
                result.getActualInstance());
    }
}
