/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.mgrtomaszzurawski.allegro.client.model.AutomaticPricingRuleConfigurationChangeByAmountRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AutomaticPricingRuleConfigurationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CompatibilityListIdItemRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CompatibilityListManualRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CompatibilityListProductOfferResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferStatusItemDtoActualPriceReductionRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.StrictOneOfModule;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullableModule;

/**
 * Pins the strict {@code oneOf} resolution. {@code AutomaticPricingRuleConfigurationRaw}
 * is a {@code oneOf[changeByAmount, changeByPercentage]}; a {@code {"changeByAmount":{}}}
 * payload has a property foreign to the percentage branch. Under the SDK's lenient
 * mapper both branches falsely match ("2 classes match"); the module makes only the
 * amount branch match. Also covers a oneOf used as a list element (the contextual-type
 * resolution) and the Object catch-all fallback.
 */
class StrictOneOfModuleTest {

    private static final String TEST_CHANGE_BY_AMOUNT_JSON = "{\"changeByAmount\":{}}";
    private static final String TEST_MANUAL_LIST_JSON =
            "{\"items\":[{\"type\":\"ID\",\"id\":\"123\"}]}";
    private static final String TEST_SCALAR_JSON = "123";
    private static final String TEST_NESTED_MANUAL_JSON =
            "{\"type\":\"MANUAL\",\"items\":[{\"type\":\"ID\",\"id\":\"123\"}]}";
    private static final String OVER_MATCH_MARKER = "classes match";

    /** The SDK mapper config WITHOUT the strict-oneOf module (forward-compat lenient). */
    private static ObjectMapper lenientBaseMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new JsonNullableModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private static ObjectMapper strictMapper() {
        return lenientBaseMapper().registerModule(new StrictOneOfModule());
    }

    @Test
    void deserialize_whenStructuralOneOfWithoutModule_overMatchesAndFails() {
        // given — the lenient mapper the generated deserializer trials branches with
        ObjectMapper lenient = lenientBaseMapper();

        // when / then — the percentage branch also "matches" by ignoring changeByAmount,
        // so the generated deserializer throws "2 classes match, expected 1"
        IOException failure = assertThrows(IOException.class, () ->
                lenient.readValue(TEST_CHANGE_BY_AMOUNT_JSON, AutomaticPricingRuleConfigurationRaw.class));
        assertTrue(failure.getMessage().contains(OVER_MATCH_MARKER),
                "expected an over-match failure, got: " + failure.getMessage());
    }

    @Test
    void deserialize_whenStructuralOneOfWithModule_resolvesTheMatchingBranch() throws IOException {
        // given — the same mapper plus the strict-oneOf module
        ObjectMapper strict = strictMapper();

        // when
        AutomaticPricingRuleConfigurationRaw result =
                strict.readValue(TEST_CHANGE_BY_AMOUNT_JSON, AutomaticPricingRuleConfigurationRaw.class);

        // then — only the amount branch survives strict matching, no over-match
        assertInstanceOf(AutomaticPricingRuleConfigurationChangeByAmountRaw.class,
                result.getActualInstance());
    }

    @Test
    void deserialize_whenOneOfIsAListElement_resolvesEachElement() throws IOException {
        // given — a parent whose `items` is a List<oneOf wrapper>; contextual-type
        // resolution must yield the element wrapper, not the List type
        ObjectMapper strict = strictMapper();

        // when
        CompatibilityListManualRaw manual =
                strict.readValue(TEST_MANUAL_LIST_JSON, CompatibilityListManualRaw.class);

        // then — the id item (unique `id` property) resolves, no "Cannot resolve oneOf List"
        assertEquals(1, manual.getItems().size());
        assertInstanceOf(CompatibilityListIdItemRaw.class,
                manual.getItems().get(0).getActualInstance());
    }

    @Test
    void deserialize_whenCandidateContainsNestedOneOf_resolvesThroughReaderCodec() throws IOException {
        // given — a oneOf whose Manual branch has items:List<CompatibilityListItemRaw>,
        // itself a strict wrapper; that nested trial runs under an ObjectReader codec
        ObjectMapper strict = strictMapper();

        // when
        CompatibilityListProductOfferResponseRaw result =
                strict.readValue(TEST_NESTED_MANUAL_JSON, CompatibilityListProductOfferResponseRaw.class);

        // then — the Manual branch resolves; the nested wrapper is not swallowed as a
        // ClassCastException miss (the codec-cast bug)
        assertInstanceOf(CompatibilityListManualRaw.class, result.getActualInstance());
    }

    @Test
    void deserialize_whenNoTypedBranchMatches_fallsBackToObjectBranch() throws IOException {
        // given — a oneOf[Dto, Object] and a scalar the typed branch cannot accept
        ObjectMapper strict = strictMapper();

        // when
        OfferStatusItemDtoActualPriceReductionRaw result =
                strict.readValue(TEST_SCALAR_JSON, OfferStatusItemDtoActualPriceReductionRaw.class);

        // then — no typed match, so the Object catch-all branch wins
        assertInstanceOf(Integer.class, result.getActualInstance());
    }
}
