/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import org.jspecify.annotations.Nullable;

/**
 * Who is accountable when returned goods come back non-sellable — the warehouse,
 * the buyer, or nobody. Allegro documents this as an open value set, so an
 * unrecognized token maps to {@link #UNKNOWN}.
 *
 * @since 0.3.0
 */
public enum AccountableParty {

    /** The fulfillment warehouse is accountable. */
    WAREHOUSE("WAREHOUSE"),

    /** The buyer is accountable. */
    BUYER("BUYER"),

    /** No party is accountable (e.g. the goods are sellable). */
    NOT_APPLICABLE("NOT_APPLICABLE"),

    /** A value Allegro introduced after this SDK build (open value set). */
    UNKNOWN(null);

    private final @Nullable String wireValue;

    AccountableParty(@Nullable String wireValue) {
        this.wireValue = wireValue;
    }

    /** The exact token Allegro uses on the wire, or {@code null} for {@link #UNKNOWN}. */
    public @Nullable String wireValue() {
        return wireValue;
    }

    /**
     * Resolve the enum from the wire token. The set is open, so an unrecognized
     * token maps to {@link #UNKNOWN} instead of throwing.
     */
    public static AccountableParty fromWire(String wireValue) {
        for (AccountableParty party : values()) {
            if (wireValue.equals(party.wireValue)) {
                return party;
            }
        }
        return UNKNOWN;
    }
}
