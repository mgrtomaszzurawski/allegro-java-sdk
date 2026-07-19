/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

/**
 * Per-offer outcome within an Allegro Prices submit or exclusion command. The SDK
 * polls the command until every offer is terminal ({@link #SUCCESS} or
 * {@link #FAILED}); {@link #IN_PROGRESS} is only observed mid-poll.
 *
 * @since 0.2.0
 */
public enum SubsidyOfferStatus {

    /** The offer was submitted to / excluded from Allegro Prices successfully. */
    SUCCESS,

    /** Still being processed (transient — not seen on a returned report). */
    IN_PROGRESS,

    /** The offer could not be processed — see {@link SubsidyOfferResult#errors()}. */
    FAILED,

    /** A value Allegro introduced that this SDK version does not model yet (read-only forward-compat sentinel). */
    UNKNOWN;

    /**
     * Map the Allegro wire value (identical to the constant name) to the enum,
     * degrading a value Allegro added after this SDK version to {@link #UNKNOWN}
     * rather than failing the read.
     */
    static SubsidyOfferStatus from(String wireValue) {
        try {
            return valueOf(wireValue);
        } catch (IllegalArgumentException unmodelledValue) {
            return UNKNOWN;
        }
    }
}
