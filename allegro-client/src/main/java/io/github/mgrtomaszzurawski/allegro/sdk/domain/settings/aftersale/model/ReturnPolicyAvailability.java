/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model;

import io.github.mgrtomaszzurawski.allegro.client.model.RestrictionCauseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ReturnPolicyAvailabilityRaw;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * The availability of returns under a return policy: a {@link ReturnRange} and,
 * when the range is {@code RESTRICTED} or {@code DISABLED}, the
 * {@link ReturnRestrictionCause} behind it.
 *
 * @param range whether returns are full, restricted or disabled
 * @param restrictionCause the restriction reason, or {@code null} for {@code FULL}
 * @param restrictionDescription server-provided restriction detail, or {@code null}
 *
 * @since 0.3.0
 */
public record ReturnPolicyAvailability(
        ReturnRange range,
        @Nullable ReturnRestrictionCause restrictionCause,
        @Nullable String restrictionDescription) {

    private static final String ERR_RANGE = "range is required";
    private static final String ERR_CAUSE = "restrictionCause is required for a restricted/disabled range";

    /** Canonical constructor — {@code range} is required. */
    public ReturnPolicyAvailability {
        Objects.requireNonNull(range, ERR_RANGE);
    }

    /** Full return availability (no restriction). */
    public static ReturnPolicyAvailability full() {
        return new ReturnPolicyAvailability(ReturnRange.FULL, null, null);
    }

    /** Restricted availability for the given cause. */
    public static ReturnPolicyAvailability restricted(ReturnRestrictionCause cause) {
        return new ReturnPolicyAvailability(ReturnRange.RESTRICTED,
                Objects.requireNonNull(cause, ERR_CAUSE), null);
    }

    /** Returns disabled for the given cause. */
    public static ReturnPolicyAvailability disabled(ReturnRestrictionCause cause) {
        return new ReturnPolicyAvailability(ReturnRange.DISABLED,
                Objects.requireNonNull(cause, ERR_CAUSE), null);
    }

    /** Map the generated Layer-1 DTO to the public record. */
    public static ReturnPolicyAvailability from(ReturnPolicyAvailabilityRaw raw) {
        RestrictionCauseRaw cause = raw.getRestrictionCause();
        ReturnRestrictionCause mappedCause = null;
        String description = null;
        if (cause != null) {
            mappedCause = cause.getName() == null ? null : ReturnRestrictionCause.from(cause.getName());
            description = cause.getDescription();
        }
        return new ReturnPolicyAvailability(ReturnRange.from(raw.getRange()), mappedCause, description);
    }
}
