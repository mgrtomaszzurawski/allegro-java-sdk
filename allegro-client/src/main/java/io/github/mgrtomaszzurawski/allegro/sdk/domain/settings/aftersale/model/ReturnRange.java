/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ReturnPolicyAvailabilityRaw;

/**
 * Whether returns under a policy are fully available, restricted to certain
 * causes, or disabled.
 *
 * @since 0.3.0
 */
public enum ReturnRange {

    /** Returns are fully available. */
    FULL,

    /** Returns are available but restricted (a {@link ReturnRestrictionCause} applies). */
    RESTRICTED,

    /** Returns are disabled. */
    DISABLED;

    /**
     * Map the generated Layer-1 enum to the public domain enum. The wire value
     * and the constant name coincide, so an unmapped server value fails loudly
     * via {@link #valueOf(String)} rather than being swallowed.
     */
    public static ReturnRange from(ReturnPolicyAvailabilityRaw.RangeEnum raw) {
        return valueOf(raw.name());
    }
}
