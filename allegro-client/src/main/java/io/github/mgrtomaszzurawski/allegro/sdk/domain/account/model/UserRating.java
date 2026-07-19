/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model;

import io.github.mgrtomaszzurawski.allegro.client.model.OrderRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RatesRaw;
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
 * @param excludedFromAverageRatesReason why this rating is excluded from averages,
 *     or {@code null} if it is not excluded or no reason was reported
 * @param rates the buyer's per-category rating scores, or {@code null} if none
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
        @Nullable String excludedFromAverageRatesReason,
        @Nullable Rates rates,
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
                raw.getExcludedFromAverageRatesReason(),
                Rates.from(raw.getRates()),
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

    /**
     * The buyer's per-category rating scores, each on a 1&ndash;5 scale.
     *
     * <p>A category the server reports with a value outside the 1&ndash;5 range
     * (including the OpenAPI forward-compatibility sentinel) maps to {@code null}
     * rather than a bogus number.
     *
     * @param delivery delivery-time score (1&ndash;5), or {@code null}
     * @param deliveryCost delivery-cost score (1&ndash;5), or {@code null}
     * @param description offer-description accuracy score (1&ndash;5), or {@code null}
     * @param service customer-service score (1&ndash;5), or {@code null}
     */
    public record Rates(
            @Nullable Integer delivery,
            @Nullable Integer deliveryCost,
            @Nullable Integer description,
            @Nullable Integer service) {

        private static final int MIN_RATE = 1;
        private static final int MAX_RATE = 5;

        static @Nullable Rates from(@Nullable RatesRaw raw) {
            if (raw == null) {
                return null;
            }
            return new Rates(
                    rate(raw.getDelivery() == null ? null : raw.getDelivery().getValue()),
                    rate(raw.getDeliveryCost() == null ? null : raw.getDeliveryCost().getValue()),
                    rate(raw.getDescription() == null ? null : raw.getDescription().getValue()),
                    rate(raw.getService() == null ? null : raw.getService().getValue()));
        }

        /** A wire score kept only when it is a real 1&ndash;5 rating, else {@code null}. */
        private static @Nullable Integer rate(@Nullable Integer wireValue) {
            return wireValue != null && wireValue >= MIN_RATE && wireValue <= MAX_RATE ? wireValue : null;
        }
    }
}
