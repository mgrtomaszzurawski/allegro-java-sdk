/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.NamedReference;
import org.junit.jupiter.api.Test;

/** The id-or-name reference value object. */
class NamedReferenceTest {

    private static final String ID = "ref-1";
    private static final String NAME = "Reference name";

    @Test
    void byId_setsIdAndLeavesNameNull() {
        NamedReference reference = NamedReference.byId(ID);
        assertEquals(ID, reference.id());
        assertNull(reference.name());
    }

    @Test
    void byName_setsNameAndLeavesIdNull() {
        NamedReference reference = NamedReference.byName(NAME);
        assertEquals(NAME, reference.name());
        assertNull(reference.id());
    }

    @Test
    void byId_whenNull_throws() {
        assertThrows(NullPointerException.class, () -> NamedReference.byId(null));
    }

    @Test
    void byName_whenNull_throws() {
        assertThrows(NullPointerException.class, () -> NamedReference.byName(null));
    }

    @Test
    void constructor_whenBothIdAndName_throws() {
        assertThrows(IllegalArgumentException.class, () -> new NamedReference(ID, NAME));
    }

    @Test
    void constructor_whenNeitherIdNorName_throws() {
        assertThrows(IllegalArgumentException.class, () -> new NamedReference(null, null));
    }
}
