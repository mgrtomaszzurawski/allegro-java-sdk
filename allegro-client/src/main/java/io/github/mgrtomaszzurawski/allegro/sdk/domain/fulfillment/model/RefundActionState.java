/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import org.jspecify.annotations.Nullable;

/**
 * Whether a refund disposition needs the seller to act. Allegro documents this
 * as an open value set, so an unrecognized token maps to {@link #UNKNOWN}.
 *
 * @since 0.3.0
 */
public enum RefundActionState {

    /** Nothing is required from the seller. */
    NO_ACTION_NEEDED("NO_ACTION_NEEDED"),

    /** The seller needs to take an action. */
    ACTION_NEEDED("ACTION_NEEDED"),

    /** An action is already under way. */
    IN_PROGRESS("IN_PROGRESS"),

    /** A state Allegro introduced after this SDK build (open value set). */
    UNKNOWN(null);

    private final @Nullable String wireValue;

    RefundActionState(@Nullable String wireValue) {
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
    public static RefundActionState fromWire(String wireValue) {
        for (RefundActionState state : values()) {
            if (wireValue.equals(state.wireValue)) {
                return state;
            }
        }
        return UNKNOWN;
    }
}
