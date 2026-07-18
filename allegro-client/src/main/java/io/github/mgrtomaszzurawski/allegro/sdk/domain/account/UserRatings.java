/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.builder.RatingAnswer;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.builder.RatingFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.builder.RatingRemoval;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.Answer;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.Removal;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.UserRating;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.UserRatingSummary;
import java.util.stream.Stream;

/**
 * The authenticated seller's received ratings — reached via
 * {@code AllegroClient.user().ratings()}. Reads and writes need the
 * {@code ratings} scope; {@link #summaryOf(String)} reads any user's public
 * summary and needs only {@code profile:read}.
 *
 * @since 0.2.0
 */
public interface UserRatings {

    /**
     * Lazily stream the seller's received ratings, newest first. Pages are
     * fetched on demand as the stream is consumed.
     *
     * @param filter selection criteria ({@link RatingFilter#all()} for all)
     * @return a lazy stream of ratings
     */
    Stream<UserRating> stream(RatingFilter filter);

    /**
     * A single received rating by id.
     *
     * @param ratingId the rating id
     * @return the rating
     */
    UserRating get(String ratingId);

    /**
     * Publish an answer to a received rating.
     *
     * @param ratingId the rating to answer
     * @param answer the answer to publish
     * @return the published answer
     */
    Answer answer(String ratingId, RatingAnswer answer);

    /**
     * Ask Allegro to remove a received rating.
     *
     * @param ratingId the rating to request removal of
     * @param removal the removal request with its explanation
     * @return the resulting removal state
     */
    Removal requestRemoval(String ratingId, RatingRemoval removal);

    /**
     * Any user's public ratings summary.
     *
     * @param userId the user whose summary to fetch
     * @return the ratings summary
     */
    UserRatingSummary summaryOf(String userId);
}
