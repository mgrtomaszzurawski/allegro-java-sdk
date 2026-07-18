/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model;

import io.github.mgrtomaszzurawski.allegro.client.model.OrderRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.UserRatingRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.UserRaw;
import org.jspecify.annotations.Nullable;

/**
 * A rating the authenticated seller received from a buyer, as returned by
 * {@code UserRatings.get(...)} and {@code UserRatings.stream(...)}.
 *
 * @param id rating identifier
 * @param buyer the buyer who left the rating
 * @param recommended whether the buyer recommends the purchase
 * @param comment the buyer's comment, or {@code null} if none
 * @param createdAt when the rating was created (ISO-8601)
 * @param editedAt when the rating was last edited (ISO-8601), or {@code null}
 * @param lastChangedAt when the rating last changed (ISO-8601), or {@code null}
 * @param answer the seller's published answer, or {@code null} if none
 * @param removal the removal state, or {@code null} if not applicable
 * @param excludedFromAverageRates whether this rating is excluded from averages
 * @param orderId id of the order the rating concerns, or {@code null}
 *
 * @since 0.2.0
 */
public record UserRating(
        String id,
        Buyer buyer,
        boolean recommended,
        @Nullable String comment,
        String createdAt,
        @Nullable String editedAt,
        @Nullable String lastChangedAt,
        @Nullable Answer answer,
        @Nullable Removal removal,
        boolean excludedFromAverageRates,
        @Nullable String orderId) {

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static UserRating from(UserRatingRaw raw) {
        UserRaw buyer = raw.getBuyer();
        OrderRaw order = raw.getOrder();
        return new UserRating(
                raw.getId(),
                new Buyer(buyer.getId(), buyer.getLogin()),
                Boolean.TRUE.equals(raw.getRecommended()),
                raw.getComment(),
                raw.getCreatedAt(),
                raw.getEditedAt(),
                raw.getLastChangedAt(),
                raw.getAnswer() == null ? null : Answer.from(raw.getAnswer()),
                raw.getRemoval() == null ? null : Removal.from(raw.getRemoval()),
                Boolean.TRUE.equals(raw.getExcludedFromAverageRates()),
                order == null ? null : order.getId());
    }

    /**
     * The buyer who left a rating.
     *
     * @param id buyer's user id
     * @param login buyer's public login
     */
    public record Buyer(String id, String login) {
    }
}
