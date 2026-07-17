/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.account;

import io.github.mgrtomaszzurawski.allegro.client.model.FundraisingCampaignRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.FundraisingCampaignsRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.Charity;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.builder.CharitySearch;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.FundraisingCampaign;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.List;

/**
 * Endpoint wrapper behind the {@link Charity} facade (beta media type).
 *
 * @since 0.2.0
 */
public final class CharityImpl implements Charity {

    private static final String OP_SEARCH = "search fundraising campaigns";
    private static final String QUERY_PHRASE = "phrase";
    private static final String QUERY_LIMIT = "limit";

    private final HttpSupport http;

    public CharityImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public List<FundraisingCampaign> searchCampaigns(CharitySearch search) {
        FundraisingCampaignsRaw response = http.request(OP_SEARCH)
                .get(ApiPaths.CHARITY_CAMPAIGNS)
                .query(Query.create()
                        .add(QUERY_PHRASE, search.phrase())
                        .add(QUERY_LIMIT, search.limit()))
                .acceptBeta()
                .fetch(FundraisingCampaignsRaw.class);
        List<FundraisingCampaignRaw> campaigns = response.getCampaigns();
        if (campaigns == null) {
            return List.of();
        }
        return campaigns.stream().map(FundraisingCampaign::from).toList();
    }
}
