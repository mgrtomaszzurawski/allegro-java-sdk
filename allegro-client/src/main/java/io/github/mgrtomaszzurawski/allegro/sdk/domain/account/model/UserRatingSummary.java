/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model;

import io.github.mgrtomaszzurawski.allegro.client.model.UserRatingSummaryResponseV2Raw;
import io.github.mgrtomaszzurawski.allegro.client.model.UserRatingSummaryResponseV2NotRecommendedRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.UserRatingSummaryResponseV2RecommendedRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.UserRatingSummaryResponseV2StatisticsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.UserRatingSummaryResponseV2StatisticsExcludedRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.UserRatingSummaryResponseV2StatisticsReceivedRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.UserRatingSummaryResponseV2StatisticsRemovedRaw;
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
 * @param statistics received/excluded/removed rating counts, or {@code null} if
 *     the server did not report the breakdown
 *
 * @since 0.2.0
 */
public record UserRatingSummary(
        RatingCount recommended,
        RatingCount notRecommended,
        @Nullable String recommendedPercentage,
        @Nullable LocalDate userSince,
        @Nullable Statistics statistics) {

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static UserRatingSummary from(UserRatingSummaryResponseV2Raw raw) {
        UserRatingSummaryResponseV2UserRaw user = raw.getUser();
        return new UserRatingSummary(
                RatingCount.from(raw.getRecommended()),
                RatingCount.from(raw.getNotRecommended()),
                raw.getRecommendedPercentage(),
                user == null ? null : user.getCreatedAt(),
                Statistics.from(raw.getStatistics()));
    }

    /** A nullable server count treated as zero when absent. */
    private static long orZero(@Nullable Long value) {
        return value == null ? 0L : value;
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
    }

    /**
     * The received/excluded/removed rating counts behind the summary.
     *
     * @param receivedTotal total number of ratings received
     * @param excludedTotal number of ratings excluded from the average
     * @param removed how removed ratings break down by who removed them
     *
     * @since 0.2.0
     */
    public record Statistics(long receivedTotal, long excludedTotal, Removed removed) {

        static @Nullable Statistics from(@Nullable UserRatingSummaryResponseV2StatisticsRaw raw) {
            if (raw == null) {
                return null;
            }
            UserRatingSummaryResponseV2StatisticsReceivedRaw received = raw.getReceived();
            UserRatingSummaryResponseV2StatisticsExcludedRaw excluded = raw.getExcluded();
            return new Statistics(
                    received == null ? 0L : orZero(received.getTotal()),
                    excluded == null ? 0L : orZero(excluded.getTotal()),
                    Removed.from(raw.getRemoved()));
        }

        /**
         * How removed ratings break down by the party that removed them.
         *
         * @param total total number of removed ratings
         * @param byAdmin ratings removed by an Allegro administrator
         * @param byBuyer ratings removed by the buyer
         * @param byBuyerDueToCompensation ratings the buyer removed after compensation
         *
         * @since 0.2.0
         */
        public record Removed(long total, long byAdmin, long byBuyer, long byBuyerDueToCompensation) {

            static Removed from(@Nullable UserRatingSummaryResponseV2StatisticsRemovedRaw raw) {
                if (raw == null) {
                    return new Removed(0L, 0L, 0L, 0L);
                }
                return new Removed(
                        orZero(raw.getTotal()),
                        orZero(raw.getByAdmin()),
                        orZero(raw.getByBuyer()),
                        orZero(raw.getByBuyerDueToCompensation()));
            }
        }
    }
}
