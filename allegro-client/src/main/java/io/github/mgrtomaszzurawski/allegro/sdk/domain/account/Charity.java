/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.builder.CharitySearch;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.FundraisingCampaign;
import java.util.List;

/**
 * Charity fundraising-campaign search — reached via {@code AllegroClient.charity()}.
 * Beta resource.
 *
 * @since 0.2.0
 */
public interface Charity {

    /**
     * Search fundraising campaigns by name/organization prefix.
     *
     * @param search the search criteria (phrase required, bounded result limit)
     * @return the matching campaigns; never {@code null}, possibly empty
     */
    List<FundraisingCampaign> searchCampaigns(CharitySearch search);
}
