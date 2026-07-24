/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mgrtomaszzurawski.allegro.client.model.MessageToSellerSettingsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PaymentsRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.MessageToSellerSettings;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferPayments;
import org.junit.jupiter.api.Test;

/** Null-safety + projection of the offer response metadata value types. */
class OfferResponseMetadataTest {

    private static final String HINT = "Add a note";

    @Test
    void messageToSellerSettings_whenNull_returnsNull() {
        assertNull(MessageToSellerSettings.from(null));
    }

    @Test
    void messageToSellerSettings_whenPopulated_mapsModeAndHint() {
        MessageToSellerSettingsRaw raw = new MessageToSellerSettingsRaw()
                .mode(MessageToSellerSettingsRaw.ModeEnum.OPTIONAL).hint(HINT);
        MessageToSellerSettings settings = MessageToSellerSettings.from(raw);
        assertEquals("OPTIONAL", settings.mode());
        assertEquals(HINT, settings.hint());
    }

    @Test
    void payments_whenNull_returnsNull() {
        assertNull(OfferPayments.from(null));
    }

    @Test
    void payments_whenPopulated_mapsInvoiceType() {
        PaymentsRaw raw = new PaymentsRaw().invoice(PaymentsRaw.InvoiceEnum.VAT);
        OfferPayments payments = OfferPayments.from(raw);
        assertEquals("VAT", payments.invoice());
    }
}
