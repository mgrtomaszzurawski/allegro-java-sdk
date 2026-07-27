/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mgrtomaszzurawski.allegro.client.model.OfferStatusRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferStatus;
import org.junit.jupiter.api.Test;

class OfferStatusTest {

    @Test
    void toRaw_whenClientSettableStatus_mapsToTheGeneratedValue() {
        // then — the three statuses a client may request map straight through
        assertEquals(OfferStatusRaw.ACTIVE, OfferStatus.ACTIVE.toRaw());
        assertEquals(OfferStatusRaw.INACTIVE, OfferStatus.INACTIVE.toRaw());
        assertEquals(OfferStatusRaw.ENDED, OfferStatus.ENDED.toRaw());
    }

    @Test
    void toRaw_whenActivatingTransientState_throwsIllegalArgument() {
        // ACTIVATING is a server-only transient state, not something a client can request
        assertThrows(IllegalArgumentException.class, OfferStatus.ACTIVATING::toRaw);
    }

    @Test
    void toRaw_whenUnknown_throwsIllegalArgument() {
        // UNKNOWN is not a real status a client can request
        assertThrows(IllegalArgumentException.class, OfferStatus.UNKNOWN::toRaw);
    }
}
