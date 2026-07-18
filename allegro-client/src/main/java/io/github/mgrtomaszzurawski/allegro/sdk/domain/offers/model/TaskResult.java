/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CommandTaskRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferIdRaw;
import org.jspecify.annotations.Nullable;

/**
 * The outcome of a batch command for one offer — reached from
 * {@link BatchReport#tasks()}.
 *
 * @param offerId the offer this task acted on, or {@code null} if the server
 *     reported a task with no offer reference
 * @param status  the task status token as reported by Allegro (e.g. success or
 *     an error state), or {@code null}
 * @param message a human-readable detail for a failed task, or {@code null}
 * @since 0.2.0
 */
public record TaskResult(
        @Nullable String offerId,
        @Nullable String status,
        @Nullable String message) {

    /** Project a generated command task onto the consumer record. */
    public static TaskResult from(CommandTaskRaw raw) {
        OfferIdRaw offer = raw.getOffer();
        return new TaskResult(
                offer == null ? null : offer.getId(),
                raw.getStatus(),
                raw.getMessage());
    }
}
