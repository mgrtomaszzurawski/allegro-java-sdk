/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

/**
 * Whether the seller's account participates in Allegro Prices on a marketplace.
 *
 * @since 0.2.0
 */
public enum ParticipationStatus {

    /** The account takes part in Allegro Prices on the marketplace. */
    ALLOWED,

    /** The account does not take part in Allegro Prices on the marketplace. */
    DENIED,

    /**
     * A value Allegro introduced that this SDK version does not model yet. It is a
     * read-only forward-compat sentinel — it degrades an unknown wire status on a
     * read; the {@code updateParticipation} builder only ever emits {@link #ALLOWED}
     * or {@link #DENIED}, so it is never sent back on a write.
     */
    UNKNOWN;

    /**
     * Map the Allegro wire value (identical to the constant name) to the enum,
     * degrading a value Allegro added after this SDK version to {@link #UNKNOWN}
     * rather than failing the read.
     */
    static ParticipationStatus from(String wireValue) {
        try {
            return valueOf(wireValue);
        } catch (IllegalArgumentException unmodelledValue) {
            return UNKNOWN;
        }
    }
}
