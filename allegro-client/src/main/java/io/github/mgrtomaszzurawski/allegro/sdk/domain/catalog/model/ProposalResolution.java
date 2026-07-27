/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import org.jspecify.annotations.Nullable;

/**
 * How Allegro resolved one proposed field of a {@link ProductChangeProposal}.
 *
 * @since 0.2.0
 */
public enum ProposalResolution {

    /** Not yet reviewed. */
    UNRESOLVED,

    /** The proposed change was accepted. */
    ACCEPTED,

    /** The proposed change was rejected. */
    REJECTED,

    /**
     * A resolution this SDK release does not model yet — a forward-compat sentinel for
     * a value Allegro introduced after this version.
     */
    UNKNOWN;

    /** Map Allegro's wire resolution value to the domain enum, degrading unknowns to {@link #UNKNOWN}. */
    public static ProposalResolution from(@Nullable String wireResolution) {
        if (wireResolution == null) {
            return UNKNOWN;
        }
        for (ProposalResolution candidate : values()) {
            if (candidate != UNKNOWN && candidate.name().equals(wireResolution)) {
                return candidate;
            }
        }
        return UNKNOWN;
    }
}
