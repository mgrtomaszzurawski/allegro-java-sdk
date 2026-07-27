/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mgrtomaszzurawski.allegro.client.model.MessageToSellerSettingsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PaymentsRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.InvoiceType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.MessageToSellerMode;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.MessageToSellerSettings;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferPayments;
import org.junit.jupiter.api.Test;

/** Null-safety + projection of the offer response metadata value types. */
class OfferResponseMetadataTest {

    private static final String HINT = "Add a note";
    private static final MessageToSellerMode MODE_OPTIONAL = MessageToSellerMode.OPTIONAL;
    private static final InvoiceType INVOICE_VAT = InvoiceType.VAT;

    @Test
    void messageToSellerSettings_whenNull_returnsNull() {
        // then an absent settings block projects to null, not an empty object
        assertNull(MessageToSellerSettings.from(null));
    }

    @Test
    void messageToSellerSettings_whenPopulated_mapsModeAndHint() {
        // given a settings block with a mode enum and a hint
        MessageToSellerSettingsRaw raw = new MessageToSellerSettingsRaw()
                .mode(MessageToSellerSettingsRaw.ModeEnum.OPTIONAL).hint(HINT);
        // when projected onto the consumer value
        MessageToSellerSettings settings = MessageToSellerSettings.from(raw);
        // then the mode's wire value and the hint map through
        assertEquals(MODE_OPTIONAL, settings.mode());
        assertEquals(HINT, settings.hint());
    }

    @Test
    void payments_whenNull_returnsNull() {
        // then an absent payments block projects to null
        assertNull(OfferPayments.from(null));
    }

    @Test
    void payments_whenPopulated_mapsInvoiceType() {
        // given a payments block carrying an invoice type
        PaymentsRaw raw = new PaymentsRaw().invoice(PaymentsRaw.InvoiceEnum.VAT);
        // when projected; then the invoice enum's wire value maps through
        OfferPayments payments = OfferPayments.from(raw);
        assertEquals(INVOICE_VAT, payments.invoice());
    }

    @Test
    void payments_of_thenToRaw_writesTheInvoiceEnum() {
        // given a payment settings value built for a write
        OfferPayments payments = OfferPayments.of(InvoiceType.WITHOUT_VAT);
        // when mapped to the request block; then the invoice enum is written
        assertEquals(PaymentsRaw.InvoiceEnum.WITHOUT_VAT, payments.toRaw().getInvoice());
    }

    @Test
    void payments_of_whenInvoiceNull_throws() {
        // then the write factory rejects a null invoice type
        assertThrows(NullPointerException.class, () -> OfferPayments.of(null));
    }
}
