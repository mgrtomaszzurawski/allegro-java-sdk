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
    DENIED;

    /** Map the Allegro wire value (identical to the constant name) to the enum. */
    static ParticipationStatus from(String wireValue) {
        return valueOf(wireValue);
    }
}
