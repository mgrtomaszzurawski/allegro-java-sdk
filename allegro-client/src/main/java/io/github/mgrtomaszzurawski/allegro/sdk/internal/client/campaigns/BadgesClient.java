/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.campaigns;

import io.github.mgrtomaszzurawski.allegro.client.model.GetBadgeCampaignsListRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.Badges;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.BadgeCampaign;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Endpoint wrapper behind the {@link Badges} facade (bucket H, badge campaigns).
 *
 * @since 0.2.0
 */
public final class BadgesClient implements Badges {

    private static final String OP_AVAILABLE_CAMPAIGNS = "list badge campaigns";
    private static final String PARAM_MARKETPLACE_ID = "marketplace.id";
    private static final String ERR_BLANK_MARKETPLACE = "marketplaceId must not be null or blank";

    private final HttpSupport http;

    public BadgesClient(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public List<BadgeCampaign> availableCampaigns() {
        return fetchCampaigns(null);
    }

    @Override
    public List<BadgeCampaign> availableCampaigns(String marketplaceId) {
        if (marketplaceId == null || marketplaceId.isBlank()) {
            throw new IllegalArgumentException(ERR_BLANK_MARKETPLACE);
        }
        return fetchCampaigns(marketplaceId);
    }

    private List<BadgeCampaign> fetchCampaigns(@Nullable String marketplaceId) {
        GetBadgeCampaignsListRaw raw = http.request(OP_AVAILABLE_CAMPAIGNS)
                .get(ApiPaths.BADGE_CAMPAIGNS)
                .query(Query.create().add(PARAM_MARKETPLACE_ID, marketplaceId))
                .fetch(GetBadgeCampaignsListRaw.class);
        return raw.getBadgeCampaigns().stream().map(BadgeCampaign::from).toList();
    }
}
