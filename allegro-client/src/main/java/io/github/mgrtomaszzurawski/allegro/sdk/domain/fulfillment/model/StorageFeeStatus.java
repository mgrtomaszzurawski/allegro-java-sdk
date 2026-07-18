/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import org.jspecify.annotations.Nullable;

/**
 * Whether a long-term storage fee applies to a fulfilled product. Allegro
 * documents this as an open value set, so an unrecognized token maps to
 * {@link #UNKNOWN} rather than failing the read.
 *
 * @since 0.3.0
 */
public enum StorageFeeStatus {

    /** No storage fee is charged for this product. */
    NOT_APPLICABLE("NOT_APPLICABLE"),

    /** A storage fee is being charged for this product. */
    CHARGED("CHARGED"),

    /** A status Allegro introduced after this SDK build (open value set). */
    UNKNOWN(null);

    private final @Nullable String wireValue;

    StorageFeeStatus(@Nullable String wireValue) {
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
    public static StorageFeeStatus fromWire(String wireValue) {
        for (StorageFeeStatus status : values()) {
            if (wireValue.equals(status.wireValue)) {
                return status;
            }
        }
        return UNKNOWN;
    }
}
