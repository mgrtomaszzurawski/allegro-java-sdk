/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mgrtomaszzurawski.allegro.client.model.ProductDepositRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.ProductDeposit;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The returnable-packaging deposit value object, both directions. */
class ProductDepositTest {

    private static final String DEPOSIT_ID = "b1f9d6d0-0000-4000-8000-000000000009";
    private static final int QUANTITY = 4;

    @Test
    void of_whenOnlyId_defaultsToOneUnit() {
        // when
        ProductDeposit deposit = ProductDeposit.of(DEPOSIT_ID);

        // then
        assertEquals(DEPOSIT_ID, deposit.id());
        assertEquals(1, deposit.quantity());
    }

    @Test
    void of_whenQuantityGiven_carriesIt() {
        // then
        assertEquals(QUANTITY, ProductDeposit.of(DEPOSIT_ID, QUANTITY).quantity());
    }

    @Test
    void of_whenIdNull_throws() {
        // then
        assertThrows(NullPointerException.class, () -> ProductDeposit.of(null));
    }

    @Test
    void of_whenQuantityBelowOne_throws() {
        // then
        assertThrows(IllegalArgumentException.class, () -> ProductDeposit.of(DEPOSIT_ID, 0));
    }

    @Test
    void from_mapsIdAndQuantity() {
        // given a generated deposit carrying a UUID id and a quantity
        ProductDepositRaw raw = new ProductDepositRaw().id(UUID.fromString(DEPOSIT_ID)).quantity(QUANTITY);

        // when projected
        ProductDeposit deposit = ProductDeposit.from(raw);

        // then the UUID maps to its string form and the quantity carries through
        assertEquals(DEPOSIT_ID, deposit.id());
        assertEquals(QUANTITY, deposit.quantity());
    }

    @Test
    void from_whenQuantityMissing_defaultsToOne() {
        // given a deposit with no quantity
        ProductDepositRaw raw = new ProductDepositRaw().id(UUID.fromString(DEPOSIT_ID));

        // when projected; then the quantity degrades to one
        assertEquals(1, ProductDeposit.from(raw).quantity());
    }

    @Test
    void toRaw_writesUuidAndQuantity() {
        // when
        ProductDepositRaw raw = ProductDeposit.of(DEPOSIT_ID, QUANTITY).toRaw();

        // then the string id is parsed back to a UUID and the quantity is written
        assertEquals(UUID.fromString(DEPOSIT_ID), raw.getId());
        assertEquals(QUANTITY, raw.getQuantity());
    }

    @Test
    void toRaw_whenIdNotUuid_throws() {
        // then a non-UUID id is rejected when it must become the wire UUID
        assertThrows(IllegalArgumentException.class, () -> ProductDeposit.of("not-a-uuid").toRaw());
    }
}
