/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.fulfillment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.AccountableParty;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.RefundActionState;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.RefundDispositionType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.RefundStockStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.ReserveStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.StorageFeeStatus;
import org.junit.jupiter.api.Test;

/**
 * Proof that the open-set fulfillment enums are forward-compatible: a known
 * token round-trips through {@code wireValue()}/{@code fromWire(...)}, and an
 * unrecognized token maps to {@code UNKNOWN} (whose wire value is {@code null})
 * instead of throwing — the behaviour the generated Layer-1 enums cannot provide.
 */
class FulfillmentEnumsTest {

    private static final String UNSEEN_TOKEN = "SOMETHING_ALLEGRO_ADDED_LATER";

    @Test
    void reserveStatus_roundTripsKnownAndFallsBackOnUnknown() {
        // given a token this build knows and one it does not
        // when each is resolved from the wire and rendered back
        // then the known token round-trips and the unseen one maps to UNKNOWN (wire value null)
        assertEquals(ReserveStatus.LOW_STOCK, ReserveStatus.fromWire("LOW_STOCK"));
        assertEquals("LOW_STOCK", ReserveStatus.LOW_STOCK.wireValue());
        assertEquals(ReserveStatus.UNKNOWN, ReserveStatus.fromWire(UNSEEN_TOKEN));
        assertNull(ReserveStatus.UNKNOWN.wireValue());
    }

    @Test
    void storageFeeStatus_roundTripsKnownAndFallsBackOnUnknown() {
        // given a token this build knows and one it does not
        // when each is resolved from the wire and rendered back
        // then the known token round-trips and the unseen one maps to UNKNOWN (wire value null)
        assertEquals(StorageFeeStatus.CHARGED, StorageFeeStatus.fromWire("CHARGED"));
        assertEquals("CHARGED", StorageFeeStatus.CHARGED.wireValue());
        assertEquals(StorageFeeStatus.UNKNOWN, StorageFeeStatus.fromWire(UNSEEN_TOKEN));
        assertNull(StorageFeeStatus.UNKNOWN.wireValue());
    }

    @Test
    void refundDispositionType_roundTripsKnownAndFallsBackOnUnknown() {
        // given a token this build knows and one it does not
        // when each is resolved from the wire and rendered back
        // then the known token round-trips and the unseen one maps to UNKNOWN (wire value null)
        assertEquals(RefundDispositionType.BOUNCE, RefundDispositionType.fromWire("BOUNCE"));
        assertEquals("BOUNCE", RefundDispositionType.BOUNCE.wireValue());
        assertEquals(RefundDispositionType.UNKNOWN, RefundDispositionType.fromWire(UNSEEN_TOKEN));
        assertNull(RefundDispositionType.UNKNOWN.wireValue());
    }

    @Test
    void refundStockStatus_roundTripsKnownAndFallsBackOnUnknown() {
        // given a token this build knows and one it does not
        // when each is resolved from the wire and rendered back
        // then the known token round-trips and the unseen one maps to UNKNOWN (wire value null)
        assertEquals(RefundStockStatus.ITEM_MISMATCH, RefundStockStatus.fromWire("ITEM_MISMATCH"));
        assertEquals("ITEM_MISMATCH", RefundStockStatus.ITEM_MISMATCH.wireValue());
        assertEquals(RefundStockStatus.UNKNOWN, RefundStockStatus.fromWire(UNSEEN_TOKEN));
        assertNull(RefundStockStatus.UNKNOWN.wireValue());
    }

    @Test
    void accountableParty_roundTripsKnownAndFallsBackOnUnknown() {
        // given a token this build knows and one it does not
        // when each is resolved from the wire and rendered back
        // then the known token round-trips and the unseen one maps to UNKNOWN (wire value null)
        assertEquals(AccountableParty.WAREHOUSE, AccountableParty.fromWire("WAREHOUSE"));
        assertEquals("WAREHOUSE", AccountableParty.WAREHOUSE.wireValue());
        assertEquals(AccountableParty.UNKNOWN, AccountableParty.fromWire(UNSEEN_TOKEN));
        assertNull(AccountableParty.UNKNOWN.wireValue());
    }

    @Test
    void refundActionState_roundTripsKnownAndFallsBackOnUnknown() {
        // given a token this build knows and one it does not
        // when each is resolved from the wire and rendered back
        // then the known token round-trips and the unseen one maps to UNKNOWN (wire value null)
        assertEquals(RefundActionState.IN_PROGRESS, RefundActionState.fromWire("IN_PROGRESS"));
        assertEquals("IN_PROGRESS", RefundActionState.IN_PROGRESS.wireValue());
        assertEquals(RefundActionState.UNKNOWN, RefundActionState.fromWire(UNSEEN_TOKEN));
        assertNull(RefundActionState.UNKNOWN.wireValue());
    }
}
