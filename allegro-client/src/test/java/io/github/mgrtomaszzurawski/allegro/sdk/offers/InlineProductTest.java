/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.InlineProduct;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferParameter;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The write-only inline product definition builder. */
class InlineProductTest {

    private static final String PRODUCT_NAME = "Nauka - duża książka dla małych dzieci";
    private static final String CATEGORY_ID = "66781";
    private static final String IMAGE_ONE = "https://example.test/a.jpg";
    private static final String IMAGE_TWO = "https://example.test/b.jpg";
    private static final String PARAM_ID = "223545";
    private static final String PARAM_VALUE_ID = "223545_1";

    @Test
    void builder_whenEmpty_leavesEverythingUnset() {
        // when nothing is set
        InlineProduct product = InlineProduct.builder().build();

        // then name/category are null and the lists are empty
        assertNull(product.name());
        assertNull(product.categoryId());
        assertTrue(product.images().isEmpty());
        assertTrue(product.parameters().isEmpty());
    }

    @Test
    void builder_whenNameAndCategorySet_carriesThem() {
        // when
        InlineProduct product = InlineProduct.builder().name(PRODUCT_NAME).categoryId(CATEGORY_ID).build();

        // then
        assertEquals(PRODUCT_NAME, product.name());
        assertEquals(CATEGORY_ID, product.categoryId());
    }

    @Test
    void image_accumulatesUrls() {
        // when images are added one at a time
        InlineProduct product = InlineProduct.builder().image(IMAGE_ONE).image(IMAGE_TWO).build();

        // then both are kept in order
        assertEquals(List.of(IMAGE_ONE, IMAGE_TWO), product.images());
    }

    @Test
    void images_replacesTheList() {
        // given one image set via image(), then images() replaces the whole list
        InlineProduct product = InlineProduct.builder()
                .image(IMAGE_ONE).images(List.of(IMAGE_TWO)).build();

        // then only the replacement list remains
        assertEquals(List.of(IMAGE_TWO), product.images());
    }

    @Test
    void parameter_accumulatesValues() {
        // when a dictionary parameter is added
        OfferParameter parameter = OfferParameter.dictionary(PARAM_ID, List.of(PARAM_VALUE_ID));
        InlineProduct product = InlineProduct.builder().parameter(parameter).build();

        // then it is carried
        assertEquals(1, product.parameters().size());
        assertEquals(PARAM_ID, product.parameters().get(0).id());
    }

    @Test
    void image_whenNull_throws() {
        // then a null image url is rejected fail-fast
        assertThrows(NullPointerException.class, () -> InlineProduct.builder().image(null));
    }

    @Test
    void parameters_whenNull_throws() {
        // then a null parameters list is rejected fail-fast
        assertThrows(NullPointerException.class, () -> InlineProduct.builder().parameters(null));
    }

    @Test
    void images_whenListHasNullElement_throws() {
        // then a null element inside the replacement list is rejected at the setter
        List<String> withNull = Arrays.asList(IMAGE_ONE, null);
        assertThrows(NullPointerException.class, () -> InlineProduct.builder().images(withNull));
    }

    @Test
    void parameters_whenListHasNullElement_throws() {
        // then a null element inside the replacement list is rejected at the setter
        List<OfferParameter> withNull = Arrays.asList(
                OfferParameter.dictionary(PARAM_ID, List.of(PARAM_VALUE_ID)), null);
        assertThrows(NullPointerException.class, () -> InlineProduct.builder().parameters(withNull));
    }
}
