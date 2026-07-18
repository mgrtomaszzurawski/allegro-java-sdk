/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import org.jspecify.annotations.Nullable;

/**
 * How far the warehouse has progressed in receiving an Advance Ship Notice's
 * goods. Allegro documents this as an open value set that may grow without
 * notice, so an unrecognized token maps to {@link #UNKNOWN} rather than failing
 * the read.
 *
 * @since 0.4.0
 */
public enum ReceivingStage {

    /** Goods are still being received and counted. */
    IN_PROGRESS("IN_PROGRESS"),

    /** All goods have been received. */
    COMPLETED("COMPLETED"),

    /** A stage Allegro introduced after this SDK build (open value set). */
    UNKNOWN(null);

    private final @Nullable String wireValue;

    ReceivingStage(@Nullable String wireValue) {
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
    public static ReceivingStage fromWire(String wireValue) {
        for (ReceivingStage stage : values()) {
            if (wireValue.equals(stage.wireValue)) {
                return stage;
            }
        }
        return UNKNOWN;
    }
}
