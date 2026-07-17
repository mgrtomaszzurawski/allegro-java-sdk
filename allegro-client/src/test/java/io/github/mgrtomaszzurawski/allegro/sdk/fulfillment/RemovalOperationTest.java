/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.fulfillment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.RemovalOperation;
import org.junit.jupiter.api.Test;

class RemovalOperationTest {

    @Test
    void fromWire_whenKnownToken_resolvesEnum() {
        assertEquals(RemovalOperation.WITHDRAWAL, RemovalOperation.fromWire("WITHDRAWAL"));
        assertEquals(RemovalOperation.DISPOSAL, RemovalOperation.fromWire("DISPOSAL"));
    }

    @Test
    void wireValue_matchesTheServerToken() {
        assertEquals("WITHDRAWAL", RemovalOperation.WITHDRAWAL.wireValue());
        assertEquals("DISPOSAL", RemovalOperation.DISPOSAL.wireValue());
    }

    @Test
    void fromWire_whenUnknownToken_throws() {
        assertThrows(IllegalArgumentException.class, () -> RemovalOperation.fromWire("MELT_IT_DOWN"));
    }
}
