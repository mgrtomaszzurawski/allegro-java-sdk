/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import org.jspecify.annotations.Nullable;

/**
 * Terminal outcome of an Advance Ship Notice submit command, as reported by the
 * warehouse once the command stops running. Returned by
 * {@code advanceShipNotices().submit(...)}; callers should treat {@link #FAILED}
 * as an unsuccessful submission. Allegro documents this as an open value set, so
 * an unrecognized token maps to {@link #UNKNOWN} rather than failing the read.
 *
 * @since 0.4.0
 */
public enum SubmitStatus {

    /** The command is still processing (non-terminal; not seen once submit returns). */
    RUNNING("RUNNING"),

    /** The notice was submitted successfully. */
    SUCCESSFUL("SUCCESSFUL"),

    /** The submission failed; the notice was not accepted. */
    FAILED("FAILED"),

    /** A status Allegro introduced after this SDK build (open value set). */
    UNKNOWN(null);

    private final @Nullable String wireValue;

    SubmitStatus(@Nullable String wireValue) {
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
    public static SubmitStatus fromWire(String wireValue) {
        for (SubmitStatus status : values()) {
            if (wireValue.equals(status.wireValue)) {
                return status;
            }
        }
        return UNKNOWN;
    }
}
