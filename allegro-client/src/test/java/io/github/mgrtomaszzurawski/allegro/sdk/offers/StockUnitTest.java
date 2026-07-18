/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOffersRequestStockRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.StockRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.StockUnit;
import org.junit.jupiter.api.Test;

class StockUnitTest {

    @Test
    void from_whenKnownValue_mapsIt() {
        assertEquals(StockUnit.UNIT, StockUnit.from(StockRaw.UnitEnum.UNIT));
        assertEquals(StockUnit.PAIR, StockUnit.from(StockRaw.UnitEnum.PAIR));
        assertEquals(StockUnit.SET, StockUnit.from(StockRaw.UnitEnum.SET));
    }

    @Test
    void from_whenNull_mapsToUnknown() {
        assertEquals(StockUnit.UNKNOWN, StockUnit.from(null));
    }

    @Test
    void from_whenFutureSentinel_degradesToUnknown() {
        // given — Layer 1 returns the forward-compat sentinel for a value added
        // after this SDK release (enumUnknownDefaultCase)
        assertEquals(StockUnit.UNKNOWN, StockUnit.from(StockRaw.UnitEnum.UNKNOWN_DEFAULT_OPEN_API));
    }

    @Test
    void toRaw_whenKnownValue_mapsToRequestEnum() {
        assertEquals(SaleProductOffersRequestStockRaw.UnitEnum.UNIT, StockUnit.UNIT.toRaw());
        assertEquals(SaleProductOffersRequestStockRaw.UnitEnum.PAIR, StockUnit.PAIR.toRaw());
        assertEquals(SaleProductOffersRequestStockRaw.UnitEnum.SET, StockUnit.SET.toRaw());
    }

    @Test
    void toRaw_whenUnknown_throwsBecauseSentinelIsReadOnly() {
        assertThrows(IllegalStateException.class, StockUnit.UNKNOWN::toRaw);
    }
}
