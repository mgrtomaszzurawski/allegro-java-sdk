/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * A time window a {@link BadgeCampaign} exposes — reused for the three campaign
 * periods: when offers may be submitted, when the campaign is visible in the
 * seller's tools, and when the badge is shown to buyers.
 *
 * <p>The {@code start}/{@code end} bounds are present only when {@link #type}
 * requires them (see {@link SchedulePolicyType}); for {@link SchedulePolicyType#ALWAYS}
 * and {@link SchedulePolicyType#NEVER} both are {@code null}.
 *
 * @param type  how the window is bounded
 * @param start inclusive start instant, or {@code null} when the policy has no start
 * @param end   inclusive end instant, or {@code null} when the policy has no end
 *
 * @since 0.2.0
 */
public record CampaignSchedule(
        SchedulePolicyType type,
        @Nullable OffsetDateTime start,
        @Nullable OffsetDateTime end) {

    /**
     * Build from the wire policy-type value and the two ISO-8601 timestamps.
     * The three generated time-policy DTOs share this shape, so the mapper
     * passes their common fields here rather than coupling to each Raw type.
     */
    static CampaignSchedule from(String policyType, @Nullable String startIso,
            @Nullable String endIso) {
        return new CampaignSchedule(SchedulePolicyType.from(policyType), parse(startIso), parse(endIso));
    }

    private static @Nullable OffsetDateTime parse(@Nullable String isoTimestamp) {
        return isoTimestamp == null ? null : OffsetDateTime.parse(isoTimestamp);
    }
}
