/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model;

import io.github.mgrtomaszzurawski.allegro.client.model.UserRatingSummaryResponseV2Raw;
import io.github.mgrtomaszzurawski.allegro.client.model.UserRatingSummaryResponseV2NotRecommendedRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.UserRatingSummaryResponseV2RecommendedRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.UserRatingSummaryResponseV2UserRaw;
import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

/**
 * Any user's public ratings summary, as returned by
 * {@code UserRatings.summaryOf(userId)}: how many buyers recommend the seller,
 * how many do not, and since when the account exists.
 *
 * @param recommended count of ratings recommending the seller
 * @param notRecommended count of ratings not recommending the seller
 * @param recommendedPercentage percentage recommending, as the server string
 *     (e.g. {@code "98.5"}), or {@code null}
 * @param userSince the date the account was created, or {@code null}
 *
 * @since 0.2.0
 */
public record UserRatingSummary(
        RatingCount recommended,
        RatingCount notRecommended,
        @Nullable String recommendedPercentage,
        @Nullable LocalDate userSince) {

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static UserRatingSummary from(UserRatingSummaryResponseV2Raw raw) {
        UserRatingSummaryResponseV2UserRaw user = raw.getUser();
        return new UserRatingSummary(
                RatingCount.from(raw.getRecommended()),
                RatingCount.from(raw.getNotRecommended()),
                raw.getRecommendedPercentage(),
                user == null ? null : user.getCreatedAt());
    }

    /**
     * A recommended/not-recommended tally.
     *
     * @param unique number of unique buyers
     * @param total total number of ratings
     */
    public record RatingCount(long unique, long total) {

        static RatingCount from(@Nullable UserRatingSummaryResponseV2RecommendedRaw raw) {
            if (raw == null) {
                return new RatingCount(0L, 0L);
            }
            return new RatingCount(orZero(raw.getUnique()), orZero(raw.getTotal()));
        }

        static RatingCount from(@Nullable UserRatingSummaryResponseV2NotRecommendedRaw raw) {
            if (raw == null) {
                return new RatingCount(0L, 0L);
            }
            return new RatingCount(orZero(raw.getUnique()), orZero(raw.getTotal()));
        }

        private static long orZero(@Nullable Long value) {
            return value == null ? 0L : value;
        }
    }
}
