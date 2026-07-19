/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.shipping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.LabelFormat;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.LabelPageSize;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.LabelSummaryField;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.LabelSummaryPlacement;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PackageType;
import org.junit.jupiter.api.Test;

/**
 * Read-soft / write-strict tests for the shipment-management enums: a null or
 * unmodelled wire value maps to {@code UNKNOWN}, a known value round-trips, and
 * {@code wireValue()} on {@code UNKNOWN} fails fast so a sentinel is never sent.
 */
class ShippingShipmentEnumsTest {

    private static final String GARBAGE = "NOT_A_REAL_VALUE";

    @Test
    void labelFormat_fromWire_isFailSoft_andWireValueIsStrict() {
        assertEquals(LabelFormat.UNKNOWN, LabelFormat.fromWire(null));
        assertEquals(LabelFormat.UNKNOWN, LabelFormat.fromWire(GARBAGE));
        assertEquals(LabelFormat.PDF, LabelFormat.fromWire("PDF"));
        assertEquals("ZPL", LabelFormat.ZPL.wireValue());
        assertThrows(IllegalStateException.class, LabelFormat.UNKNOWN::wireValue);
    }

    @Test
    void packageType_fromWire_isFailSoft_andWireValueIsStrict() {
        assertEquals(PackageType.UNKNOWN, PackageType.fromWire(null));
        assertEquals(PackageType.UNKNOWN, PackageType.fromWire(GARBAGE));
        assertEquals(PackageType.PALLET, PackageType.fromWire("PALLET"));
        assertEquals("PACKAGE", PackageType.PACKAGE.wireValue());
        assertThrows(IllegalStateException.class, PackageType.UNKNOWN::wireValue);
    }

    @Test
    void labelPageSize_fromWire_isFailSoft_andWireValueIsStrict() {
        assertEquals(LabelPageSize.UNKNOWN, LabelPageSize.fromWire(null));
        assertEquals(LabelPageSize.UNKNOWN, LabelPageSize.fromWire(GARBAGE));
        assertEquals(LabelPageSize.A6, LabelPageSize.fromWire("A6"));
        assertEquals("A4", LabelPageSize.A4.wireValue());
        assertThrows(IllegalStateException.class, LabelPageSize.UNKNOWN::wireValue);
    }

    @Test
    void labelSummaryPlacement_fromWire_isFailSoft_andWireValueIsStrict() {
        assertEquals(LabelSummaryPlacement.UNKNOWN, LabelSummaryPlacement.fromWire(null));
        assertEquals(LabelSummaryPlacement.UNKNOWN, LabelSummaryPlacement.fromWire(GARBAGE));
        assertEquals(LabelSummaryPlacement.EVERY, LabelSummaryPlacement.fromWire("EVERY"));
        assertEquals("LAST", LabelSummaryPlacement.LAST.wireValue());
        assertThrows(IllegalStateException.class, LabelSummaryPlacement.UNKNOWN::wireValue);
    }

    @Test
    void labelSummaryField_fromWire_isFailSoft_andWireValueIsStrict() {
        assertEquals(LabelSummaryField.UNKNOWN, LabelSummaryField.fromWire(null));
        assertEquals(LabelSummaryField.UNKNOWN, LabelSummaryField.fromWire(GARBAGE));
        assertEquals(LabelSummaryField.DIMS_AND_WEIGHT,
                LabelSummaryField.fromWire("DIMS_AND_WEIGHT"));
        assertEquals("WAYBILL", LabelSummaryField.WAYBILL.wireValue());
        assertThrows(IllegalStateException.class, LabelSummaryField.UNKNOWN::wireValue);
    }
}
