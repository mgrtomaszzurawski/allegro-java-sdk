/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.account;

import io.github.mgrtomaszzurawski.allegro.client.model.MeResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SalesQualityHistoryResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SmartSellerClassificationReportRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.AdditionalEmails;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.UserAccount;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.UserRatings;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.CurrentUser;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.SalesQuality;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.SmartClassification;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import org.jspecify.annotations.Nullable;

/**
 * Endpoint wrapper behind the {@link UserAccount} facade.
 *
 * @since 0.1.0
 */
public final class UserAccountImpl implements UserAccount {

    private static final String OP_ME = "get current user";
    private static final String OP_SALES_QUALITY = "get sales quality";
    private static final String OP_SMART = "get smart classification";
    private static final String QUERY_MARKETPLACE_ID = "marketplaceId";

    private final HttpSupport http;
    private final UserRatings ratings;
    private final AdditionalEmails additionalEmails;

    public UserAccountImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
        this.ratings = new UserRatingsImpl(runtime);
        this.additionalEmails = new AdditionalEmailsImpl(runtime);
    }

    @Override
    public CurrentUser me() {
        return CurrentUser.from(
                http.getAuthenticated(ApiPaths.CURRENT_USER, MeResponseRaw.class, OP_ME));
    }

    @Override
    public SalesQuality salesQuality() {
        return SalesQuality.from(http.getAuthenticated(
                ApiPaths.SALES_QUALITY, SalesQualityHistoryResponseRaw.class, OP_SALES_QUALITY));
    }

    @Override
    public SmartClassification smartClassification() {
        return smart(null);
    }

    @Override
    public SmartClassification smartClassification(String marketplaceId) {
        return smart(marketplaceId);
    }

    private SmartClassification smart(@Nullable String marketplaceId) {
        SmartSellerClassificationReportRaw raw = http.request(OP_SMART)
                .get(ApiPaths.SMART_CLASSIFICATION)
                .query(Query.create().add(QUERY_MARKETPLACE_ID, marketplaceId))
                .fetch(SmartSellerClassificationReportRaw.class);
        return SmartClassification.from(raw);
    }

    @Override
    public UserRatings ratings() {
        return ratings;
    }

    @Override
    public AdditionalEmails additionalEmails() {
        return additionalEmails;
    }
}
