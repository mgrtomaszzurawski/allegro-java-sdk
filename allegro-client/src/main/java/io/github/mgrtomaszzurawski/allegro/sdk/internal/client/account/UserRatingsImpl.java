/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.account;

import io.github.mgrtomaszzurawski.allegro.client.model.AnswerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RemovalRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.UserRatingAnswerRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.UserRatingListResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.UserRatingRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.UserRatingRemovalRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.UserRatingRemovalRequestRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.UserRatingSummaryResponseV2Raw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.UserRatings;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.builder.RatingAnswer;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.builder.RatingFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.builder.RatingRemoval;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.Answer;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.Removal;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.UserRating;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.UserRatingSummary;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.pagination.PagedSpliterator;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.List;
import java.util.stream.Stream;

/**
 * Endpoint wrapper behind the {@link UserRatings} facade.
 *
 * <p>The rating list response carries no {@code totalCount}, so pagination
 * terminates when a page comes back shorter than the requested page size
 * (a full page implies there may be more).
 *
 * @since 0.2.0
 */
public final class UserRatingsImpl implements UserRatings {

    private static final int PAGE_SIZE = 100;

    private static final String OP_STREAM = "stream user ratings";
    private static final String OP_GET = "get user rating";
    private static final String OP_ANSWER = "answer user rating";
    private static final String OP_REMOVAL = "request user rating removal";
    private static final String OP_SUMMARY = "get user ratings summary";

    private static final String QUERY_RECOMMENDED = "recommended";
    private static final String QUERY_CHANGED_GTE = "lastChangedAt.gte";
    private static final String QUERY_CHANGED_LTE = "lastChangedAt.lte";
    private static final String QUERY_OFFSET = "offset";
    private static final String QUERY_LIMIT = "limit";

    private final HttpSupport http;

    public UserRatingsImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public Stream<UserRating> stream(RatingFilter filter) {
        return PagedSpliterator.stream(pageIndex -> fetchPage(filter, pageIndex));
    }

    private PagedSpliterator.Page<UserRating> fetchPage(RatingFilter filter, int pageIndex) {
        Query query = Query.create()
                .add(QUERY_RECOMMENDED, filter.recommended())
                .add(QUERY_CHANGED_GTE, filter.changedFrom())
                .add(QUERY_CHANGED_LTE, filter.changedTo())
                .add(QUERY_OFFSET, pageIndex * PAGE_SIZE)
                .add(QUERY_LIMIT, PAGE_SIZE);
        UserRatingListResponseRaw response = http.request(OP_STREAM)
                .get(ApiPaths.USER_RATINGS)
                .query(query)
                .fetch(UserRatingListResponseRaw.class);
        List<UserRatingRaw> ratings = response.getRatings();
        List<UserRating> items = ratings == null
                ? List.of()
                : ratings.stream().map(UserRating::from).toList();
        boolean hasMore = items.size() == PAGE_SIZE;
        return new PagedSpliterator.Page<>(items, hasMore);
    }

    @Override
    public UserRating get(String ratingId) {
        return UserRating.from(http.getAuthenticated(
                ApiPaths.subPath(ApiPaths.USER_RATINGS, ratingId), UserRatingRaw.class, OP_GET));
    }

    @Override
    public Answer answer(String ratingId, RatingAnswer answer) {
        UserRatingAnswerRequestRaw request = new UserRatingAnswerRequestRaw().message(answer.message());
        return Answer.from(http.putJsonAuthenticated(
                ApiPaths.subPath(ApiPaths.USER_RATINGS, ratingId, ApiPaths.ANSWER_SEGMENT),
                request, AnswerRaw.class, OP_ANSWER));
    }

    @Override
    public Removal requestRemoval(String ratingId, RatingRemoval removal) {
        UserRatingRemovalRequestRaw request = new UserRatingRemovalRequestRaw().request(
                new UserRatingRemovalRequestRequestRaw().message(removal.message()));
        return Removal.from(http.putJsonAuthenticated(
                ApiPaths.subPath(ApiPaths.USER_RATINGS, ratingId, ApiPaths.REMOVAL_SEGMENT),
                request, RemovalRaw.class, OP_REMOVAL));
    }

    @Override
    public UserRatingSummary summaryOf(String userId) {
        return UserRatingSummary.from(http.getAuthenticated(
                ApiPaths.subPath(ApiPaths.USERS, userId, ApiPaths.RATINGS_SUMMARY_SEGMENT),
                UserRatingSummaryResponseV2Raw.class, OP_SUMMARY));
    }
}
