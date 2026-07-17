/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.BadgeCampaign;
import java.util.List;

/**
 * Badge campaigns — reached via {@code campaigns().badges()}.
 *
 * <p>Starter slice of bucket H: only {@link #availableCampaigns()} ships as the
 * end-to-end proof of the facade, mapping and test conventions. Application,
 * listing and update operations land with the rest of the bucket.
 *
 * @since 0.2.0
 */
public interface Badges {

    /**
     * Badge campaigns currently offered to the authenticated seller, across every
     * marketplace the account sells on.
     *
     * @return the available campaigns; never {@code null}, possibly empty
     */
    List<BadgeCampaign> availableCampaigns();

    /**
     * Badge campaigns offered to the authenticated seller on a single marketplace.
     *
     * @param marketplaceId marketplace id (e.g. {@code "allegro-pl"})
     * @return the available campaigns on that marketplace; never {@code null}, possibly empty
     */
    List<BadgeCampaign> availableCampaigns(String marketplaceId);
}
