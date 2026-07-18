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
    FINISH;

    /** Map the generated Layer-1 type enum to the public enum. */
    static BadgeOperationType from(BadgeOperationRaw.TypeEnum wireValue) {
        return valueOf(wireValue.name());
    }
}
