/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mgrtomaszzurawski.allegro.client.model.PaymentsRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.InvoiceType;
import org.junit.jupiter.api.Test;

/** Mapping of the invoice-type enum between the SDK and the generated wire enum, both ways. */
class InvoiceTypeTest {

    @Test
    void from_whenNull_returnsNull() {
        // then an absent invoice enum projects to null
        assertNull(InvoiceType.from(null));
    }

    @Test
    void from_mapsEachKnownWireValue() {
        // then every modelled wire value maps to its SDK constant
        assertEquals(InvoiceType.VAT, InvoiceType.from(PaymentsRaw.InvoiceEnum.VAT));
        assertEquals(InvoiceType.VAT_MARGIN, InvoiceType.from(PaymentsRaw.InvoiceEnum.VAT_MARGIN));
        assertEquals(InvoiceType.WITHOUT_VAT, InvoiceType.from(PaymentsRaw.InvoiceEnum.WITHOUT_VAT));
        assertEquals(InvoiceType.NO_INVOICE, InvoiceType.from(PaymentsRaw.InvoiceEnum.NO_INVOICE));
    }

    @Test
    void from_whenUnknownSentinel_mapsToUnknown() {
        // then a wire value this release does not model degrades to UNKNOWN, not an exception
        assertEquals(InvoiceType.UNKNOWN,
                InvoiceType.from(PaymentsRaw.InvoiceEnum.UNKNOWN_DEFAULT_OPEN_API));
    }

    @Test
    void toRaw_mapsEachRequestableValue() {
        // then every requestable type maps to its wire enum
        assertEquals(PaymentsRaw.InvoiceEnum.VAT, InvoiceType.VAT.toRaw());
        assertEquals(PaymentsRaw.InvoiceEnum.VAT_MARGIN, InvoiceType.VAT_MARGIN.toRaw());
        assertEquals(PaymentsRaw.InvoiceEnum.WITHOUT_VAT, InvoiceType.WITHOUT_VAT.toRaw());
        assertEquals(PaymentsRaw.InvoiceEnum.NO_INVOICE, InvoiceType.NO_INVOICE.toRaw());
    }

    @Test
    void toRaw_whenUnknown_throws() {
        // then UNKNOWN is not a value a client can request
        assertThrows(IllegalArgumentException.class, InvoiceType.UNKNOWN::toRaw);
    }
}
