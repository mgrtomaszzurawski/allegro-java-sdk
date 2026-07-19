/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import io.github.mgrtomaszzurawski.allegro.client.model.BadgeOperationRaw;

/**
 * What a badge {@link BadgeOperation} does to an offer's badge: change its
 * parameters, or end it.
 *
 * @since 0.2.0
 */
public enum BadgeOperationType {

    /** Update the badge's parameters (e.g. its bargain price). */
    UPDATE,

    /** Finish the badge, ending its display. */
    FINISH,

    /** A value Allegro introduced that this SDK version does not model yet (read-only forward-compat sentinel). */
    UNKNOWN;

    /**
     * Map the generated Layer-1 type enum to the public enum, degrading a value
     * Allegro added after this SDK version to {@link #UNKNOWN} rather than failing
     * the read.
     */
    static BadgeOperationType from(BadgeOperationRaw.TypeEnum wireValue) {
        try {
            return valueOf(wireValue.name());
        } catch (IllegalArgumentException unmodelledValue) {
            return UNKNOWN;
        }
    }
}
