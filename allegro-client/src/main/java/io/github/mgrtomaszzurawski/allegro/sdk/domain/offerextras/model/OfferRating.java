/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model;

import io.github.mgrtomaszzurawski.allegro.client.model.OfferRatingRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferRatingScoreDistributionInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferRatingSizeFeedbackInnerRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The aggregated buyer rating of an offer, as returned by
 * {@code AllegroClient.offers().rating(String)}.
 *
 * @param averageScore the average score as a decimal string (for example
 *     {@code "4.53"}), or {@code null} when the offer has no ratings yet
 * @param totalResponses the number of ratings
 * @param scoreDistribution how many responses gave each score; never
 *     {@code null}, possibly empty
 * @param sizeFeedback the size-feedback breakdown (for clothing/footwear); never
 *     {@code null}, possibly empty
 *
 * @since 0.2.0
 */
public record OfferRating(
        @Nullable String averageScore,
        int totalResponses,
        List<RatingBucket> scoreDistribution,
        List<RatingBucket> sizeFeedback) {

    private static final int NO_COUNT = 0;

    public OfferRating {
        scoreDistribution = List.copyOf(scoreDistribution);
        sizeFeedback = List.copyOf(sizeFeedback);
    }

    /** Map the generated Layer-1 rating DTO to the public record. */
    public static OfferRating from(OfferRatingRaw raw) {
        return new OfferRating(
                raw.getAverageScore(),
                count(raw.getTotalResponses()),
                scoreBuckets(raw.getScoreDistribution()),
                sizeBuckets(raw.getSizeFeedback()));
    }

    private static List<RatingBucket> scoreBuckets(
            @Nullable List<OfferRatingScoreDistributionInnerRaw> raw) {
        return raw == null
                ? List.of()
                : raw.stream()
                        .filter(bucket -> bucket.getName() != null)
                        .map(bucket -> new RatingBucket(bucket.getName(), count(bucket.getCount())))
                        .toList();
    }

    private static List<RatingBucket> sizeBuckets(
            @Nullable List<OfferRatingSizeFeedbackInnerRaw> raw) {
        return raw == null
                ? List.of()
                : raw.stream()
                        .filter(bucket -> bucket.getName() != null)
                        .map(bucket -> new RatingBucket(bucket.getName(), count(bucket.getCount())))
                        .toList();
    }

    private static int count(@Nullable Integer rawCount) {
        return rawCount == null ? NO_COUNT : rawCount;
    }
}
