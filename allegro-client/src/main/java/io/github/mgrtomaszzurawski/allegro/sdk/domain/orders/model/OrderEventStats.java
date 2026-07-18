/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model;

import io.github.mgrtomaszzurawski.allegro.client.model.LatestOrderEventRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OrderEventStatsRaw;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * A marker for the most recent order event, used to bound an event-stream walk:
 * stream events until {@link #latestEventId()} is reached rather than polling
 * indefinitely.
 *
 * @param latestEventId identifier of the newest event, or {@code null} when the
 *     seller has no order events yet
 * @param latestEventOccurredAt when the newest event happened, or {@code null}
 *
 * @since 0.4.0
 */
public record OrderEventStats(
        @Nullable String latestEventId,
        @Nullable OffsetDateTime latestEventOccurredAt) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static OrderEventStats from(OrderEventStatsRaw raw) {
        LatestOrderEventRaw latest = raw.getLatestEvent();
        return new OrderEventStats(
                latest == null ? null : latest.getId(),
                latest == null ? null : latest.getOccurredAt());
    }
}
