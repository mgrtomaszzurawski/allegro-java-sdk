/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AnswerRaw;

/**
 * A seller's public answer to a received rating, as returned by
 * {@code UserRatings.answer(...)} and carried on {@link UserRating}.
 *
 * @param createdAt when the answer was published, ISO-8601 as returned by Allegro
 * @param message the answer text
 *
 * @since 0.2.0
 */
public record Answer(String createdAt, String message) {

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static Answer from(AnswerRaw raw) {
        return new Answer(raw.getCreatedAt(), raw.getMessage());
    }
}
