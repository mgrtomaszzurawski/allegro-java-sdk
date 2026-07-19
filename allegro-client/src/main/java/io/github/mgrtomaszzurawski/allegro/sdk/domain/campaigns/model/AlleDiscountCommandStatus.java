/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

/**
 * The terminal state of an AlleDiscount submit or withdraw command. The SDK polls
 * the command to completion before returning, so only the terminal states are
 * exposed (the transient {@code NEW} / {@code IN_PROGRESS} wire states are never
 * observed on a returned result).
 *
 * @since 0.2.0
 */
public enum AlleDiscountCommandStatus {

    /** Completed successfully. */
    SUCCESSFUL,

    /** The command failed. */
    FAILED,

    /** A terminal state Allegro introduced that this SDK version does not model yet (read-only forward-compat sentinel). */
    UNKNOWN;

    /**
     * Map the Allegro wire value (identical to the constant name) to the enum,
     * degrading a value Allegro added after this SDK version to {@link #UNKNOWN}
     * rather than failing the read.
     */
    static AlleDiscountCommandStatus from(String wireValue) {
        try {
            return valueOf(wireValue);
        } catch (IllegalArgumentException unmodelledValue) {
            return UNKNOWN;
        }
    }
}
