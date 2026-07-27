/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import org.jspecify.annotations.Nullable;

/**
 * The moderation state of a {@link ProductProposal}.
 *
 * @since 0.2.0
 */
public enum ProductProposalStatus {

    /** Submitted and awaiting Allegro's verification. */
    PROPOSED,

    /** Accepted — the product is now in the catalogue. */
    LISTED,

    /**
     * A status this SDK release does not model yet — a forward-compat sentinel for a
     * value Allegro introduced after this version.
     */
    UNKNOWN;

    /** Map Allegro's wire status value to the domain enum, degrading unknowns to {@link #UNKNOWN}. */
    public static ProductProposalStatus from(@Nullable String wireStatus) {
        if (wireStatus == null) {
            return UNKNOWN;
        }
        for (ProductProposalStatus candidate : values()) {
            if (candidate != UNKNOWN && candidate.name().equals(wireStatus)) {
                return candidate;
            }
        }
        return UNKNOWN;
    }
}
