/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import io.github.mgrtomaszzurawski.allegro.client.model.BadgeProcessRaw;

/**
 * Lifecycle state of a {@link Badge} granted to one of the seller's offers, from
 * verification through active display to the end of the campaign.
 *
 * @since 0.2.0
 */
public enum BadgeStatus {

    /** The badge is being verified before it can be shown. */
    IN_VERIFICATION,

    /** Verified and queued — it will start showing when its publication window opens. */
    WAITING_FOR_PUBLICATION,

    /** Currently shown to buyers on the offer. */
    ACTIVE,

    /** The badge's publication window has ended. */
    FINISHED,

    /** The badge was rejected — see {@link Badge#rejectionReasons()}. */
    DECLINED,

    /** A value Allegro introduced that this SDK version does not model yet (read-only forward-compat sentinel). */
    UNKNOWN;

    /**
     * Map the generated Layer-1 status enum to the public enum, degrading a value
     * Allegro added after this SDK version to {@link #UNKNOWN} rather than failing
     * the read.
     */
    static BadgeStatus from(BadgeProcessRaw.StatusEnum wireValue) {
        try {
            return valueOf(wireValue.name());
        } catch (IllegalArgumentException unmodelledValue) {
            return UNKNOWN;
        }
    }
}
