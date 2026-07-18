/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import io.github.mgrtomaszzurawski.allegro.client.model.BadgeApplicationProcessRaw;

/**
 * Lifecycle state of a {@link BadgeApplication}. Allegro verifies an application
 * asynchronously (often with a human, e-mail-notified step), so a freshly created
 * application is {@link #REQUESTED} and only later becomes {@link #PROCESSED} or
 * {@link #DECLINED}.
 *
 * @since 0.2.0
 */
public enum BadgeApplicationStatus {

    /** Submitted and awaiting Allegro's verification. */
    REQUESTED,

    /** Accepted — the badge is (or will be) granted to the offer. */
    PROCESSED,

    /** Rejected — see {@link BadgeApplication#rejectionReasons()} for why. */
    DECLINED;

    /** Map the generated Layer-1 status enum to the public enum. */
    static BadgeApplicationStatus from(BadgeApplicationProcessRaw.StatusEnum wireValue) {
        return valueOf(wireValue.name());
    }
}
