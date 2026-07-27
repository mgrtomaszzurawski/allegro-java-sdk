/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CommandTaskRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ErrorRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferIdRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroFieldError;
import java.util.List;
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
 * @param field   the field the task acted on (e.g. {@code price}, {@code stock}
 *     for a field-scoped command), or {@code null}
 * @param errors  the structured per-task errors for a failed task (possibly empty)
 * @since 0.2.0
 */
public record TaskResult(
        @Nullable String offerId,
        @Nullable String status,
        @Nullable String message,
        @Nullable String field,
        List<AllegroFieldError> errors) {

    public TaskResult {
        errors = List.copyOf(errors);
    }

    /** Project a generated command task onto the consumer record. */
    public static TaskResult from(CommandTaskRaw raw) {
        OfferIdRaw offer = raw.getOffer();
        return new TaskResult(
                offer == null ? null : offer.getId(),
                raw.getStatus(),
                raw.getMessage(),
                raw.getField(),
                errorsOf(raw.getErrors()));
    }

    /** Map Allegro's structured {@code errors[]} onto the shared typed error record. */
    private static List<AllegroFieldError> errorsOf(@Nullable List<ErrorRaw> raws) {
        return raws == null
                ? List.of()
                : raws.stream()
                        .map(raw -> new AllegroFieldError(raw.getCode(), raw.getMessage(),
                                raw.getUserMessage(), raw.getPath(), raw.getDetails(), raw.getMetadata()))
                        .toList();
    }
}
