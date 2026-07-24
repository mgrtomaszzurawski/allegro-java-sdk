/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.client.model.ValidationErrorRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ValidationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ValidationWarningRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferValidation;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Projection of the offer validation block (blocking errors + non-blocking warnings). */
class OfferValidationTest {

    private static final String ERROR_CODE = "RESPONSIBLE_PRODUCER_NOT_SPECIFIED";
    private static final String ERROR_MESSAGE = "Responsible producer is required for every product";
    private static final String ERROR_PATH = "productSet[0].responsibleProducer";
    private static final String WARNING_CODE = "SAFETY_INFO_DESCRIPTION_SUGGESTED_DATA_VERIFICATION_NEEDED";

    @Test
    void from_whenNull_returnsNull() {
        // then an absent validation block projects to null
        assertNull(OfferValidation.from(null));
    }

    @Test
    void from_whenPopulated_mapsErrorsAndWarnings() {
        // given a validation block with one blocking error and one non-blocking warning
        ValidationRaw raw = new ValidationRaw()
                .addErrorsItem(new ValidationErrorRaw().code(ERROR_CODE).message(ERROR_MESSAGE).path(ERROR_PATH))
                .addWarningsItem(new ValidationWarningRaw().code(WARNING_CODE));

        // when projected onto the consumer value
        OfferValidation validation = OfferValidation.from(raw);

        // then the error and warning map through with their code/message/path (guards the
        // AllegroFieldError constructor's field order), reusing the shared error value
        assertEquals(1, validation.errors().size());
        assertEquals(ERROR_CODE, validation.errors().get(0).code());
        assertEquals(ERROR_MESSAGE, validation.errors().get(0).message());
        assertEquals(ERROR_PATH, validation.errors().get(0).path());
        assertEquals(1, validation.warnings().size());
        assertEquals(WARNING_CODE, validation.warnings().get(0).code());
    }

    @Test
    void from_whenNoErrorsOrWarnings_yieldsEmptyLists() {
        // given a clean validation (only a timestamp)
        OfferValidation validation = OfferValidation.from(new ValidationRaw());

        // then the lists are empty, never null
        assertTrue(validation.errors().isEmpty());
        assertTrue(validation.warnings().isEmpty());
    }
}
