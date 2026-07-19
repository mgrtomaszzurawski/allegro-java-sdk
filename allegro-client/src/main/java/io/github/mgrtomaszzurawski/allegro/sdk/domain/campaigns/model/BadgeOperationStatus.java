/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import io.github.mgrtomaszzurawski.allegro.client.model.BadgeOperationProcessRaw;

/**
 * Lifecycle state of a badge {@link BadgeOperation}. The SDK polls the operation
 * to a terminal state ({@link #PROCESSED} or {@link #DECLINED}) before returning
 * from {@code badges().update(...)}.
 *
 * @since 0.2.0
 */
public enum BadgeOperationStatus {

    /** Accepted for processing, not yet terminal. */
    REQUESTED,

    /** The operation completed successfully. */
    PROCESSED,

    /** The operation was rejected — see {@link BadgeOperation#rejectionReasons()}. */
    DECLINED,

    /** A value Allegro introduced that this SDK version does not model yet (read-only forward-compat sentinel). */
    UNKNOWN;

    /**
     * Map the generated Layer-1 status enum to the public enum, degrading a value
     * Allegro added after this SDK version to {@link #UNKNOWN} rather than failing
     * the read.
     */
    static BadgeOperationStatus from(BadgeOperationProcessRaw.StatusEnum wireValue) {
        try {
            return valueOf(wireValue.name());
        } catch (IllegalArgumentException unmodelledValue) {
            return UNKNOWN;
        }
    }
}
