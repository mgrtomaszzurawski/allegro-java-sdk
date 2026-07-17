/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.campaigns;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.Badges;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.Campaigns;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;

/**
 * Wiring behind the {@link Campaigns} facade — constructs and holds the
 * per-programme sub-facade clients (bucket H).
 *
 * @since 0.2.0
 */
public final class CampaignsImpl implements Campaigns {

    private final Badges badges;

    public CampaignsImpl(HttpRuntime runtime) {
        this.badges = new BadgesClient(runtime);
    }

    @Override
    public Badges badges() {
        return badges;
    }
}
